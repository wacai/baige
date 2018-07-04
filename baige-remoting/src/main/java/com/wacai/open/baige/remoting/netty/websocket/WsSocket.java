package com.wacai.open.baige.remoting.netty.websocket;

import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.remoting.netty.NettyDecoder;
import com.wacai.open.baige.remoting.netty.websocket.listener.SocketListener;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.common.frames.BinaryFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@WebSocket(maxBinaryMessageSize=10485760)
@WebSocket(maxBinaryMessageSize = 31457280)
public class WsSocket {


  private static final Logger LOGGER = LoggerFactory.getLogger(WsSocket.class);

  private NettyDecoder nettyDecoder = new NettyDecoder();
  private final ByteBufAllocator byteBufAllocator = new PooledByteBufAllocator(false);


  private final CountDownLatch closeLatch;
  private final CountDownLatch connectLatch;
  private List<SocketListener> socketListeners;

  private Session session;
  private String wsURI;
  private InetSocketAddress remoteAddr;

  private ExecutorService executorService;

  private LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue<>();


  /*用于标志建链次数*/
  private AtomicInteger connectTimes = new AtomicInteger(0);



  public WsSocket(int wsSocketThreads, String wsURI) {
    this.wsURI = wsURI;
    this.closeLatch = new CountDownLatch(1);
    this.connectLatch = new CountDownLatch(1);
    this.socketListeners = Collections.synchronizedList(new LinkedList<>());
    this.executorService = new ThreadPoolExecutor(//
        wsSocketThreads,//
        wsSocketThreads,//
        1000 * 60,//
        TimeUnit.MILLISECONDS,//
        this.linkedBlockingQueue,//
        new ThreadFactoryImpl("WsSocketThread_"));

  }


  public boolean awaitClose(int duration, TimeUnit timeUnit) throws InterruptedException {
    return this.closeLatch.await(duration, timeUnit);
  }


  public boolean awaitConnect(long duration, TimeUnit timeUnit) throws InterruptedException {
    return this.connectLatch.await(duration, timeUnit);
  }




  @OnWebSocketClose
  public void onClose(int statusCode, String reason) {
    LOGGER.warn("the websocket to {} is closed, remote addr:{}", this.wsURI, this.remoteAddr);
    this.session = null;
    this.closeLatch.countDown();


    if (socketListeners != null && socketListeners.size() > 0) {
      final WsSocket wsSocket = this;
      this.executorService.submit(new Runnable() {
        @Override
        public void run() {
          for (SocketListener socketListener : socketListeners) {
            socketListener.onWebSocketClose(wsSocket, statusCode, reason, remoteAddr);
          }
        }
      });
    }

//    if (this.executorService != null) {
//      this.executorService.shutdown();
//    }


  }


  @OnWebSocketConnect
  public void onConnect(Session session) {
    connectTimes.incrementAndGet();
    this.session = session;
    try {
      this.remoteAddr = this.session.getRemote().getInetSocketAddress();
    } catch (Exception e) {

    }


    this.connectLatch.countDown();

    final WsSocket wsSocket = this;
    if (socketListeners != null && socketListeners.size() > 0) {

      this.executorService.submit(new Runnable() {
        @Override
        public void run() {
          for (SocketListener socketListener : socketListeners) {
            try {
              socketListener.onWebSocketConnect(wsSocket, session, connectTimes.get());
            } catch (Exception e) {
              LOGGER.warn("socketListener.onWebSocketConnect catch Exception", e);
            }
          }
        }
      });
    }


  }


  @OnWebSocketError
  public void onWebSocketError(Session session, Throwable error) {
    final WsSocket wsSocket = this;
    if (socketListeners != null && socketListeners.size() > 0) {
      this.executorService.submit(new Runnable() {
        @Override
        public void run() {
          for (SocketListener socketListener : socketListeners) {
            socketListener.onWebSocketError(wsSocket, error);
          }
        }
      });
    }
  }


  @OnWebSocketFrame
  public void onReceiveFrame(Frame frame) {

    if (frame instanceof BinaryFrame) {
      BinaryFrame binaryFrame = (BinaryFrame)frame;
      ByteBuffer byteBuffer = binaryFrame.getPayload();

      ByteBuf byteBuf = byteBufAllocator.heapBuffer();
      byteBuf.writeBytes(byteBuffer);


      Object obj = null;
      try {
        obj = nettyDecoder.decode(null,byteBuf);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        byteBuf.release();
      }

      if (obj == null) {
        //未解码到完整的 command;
      } else {
        //解码得到完整的command;
        RemotingCommand cmd = (RemotingCommand) obj;

        if (socketListeners != null && socketListeners.size() > 0) {
          final WsSocket wsSocket = this;
          this.executorService.submit(new Runnable() {
            @Override
            public void run() {
              for (SocketListener socketListener : socketListeners) {
                socketListener.onRecvCommand(wsSocket, cmd);
              }
            }
          });
        }


      }

    }
  }





  /***
   * 发送request 给对端服务器。
   * @param remotingCommand
   */
  public Future sendRemotingCommand(RemotingCommand remotingCommand) throws IOException {
    ByteBuffer byteBuffer = remotingCommand.encode();
    Future<Void> future;
    future = session.getRemote().sendBytesByFuture(byteBuffer);
    return future;

  }



  public void registerSocketListener(SocketListener socketListener) {
    socketListeners.add(socketListener);
  }


  public void close() {
    if (this.executorService != null) {
      this.executorService.shutdown();
    }
  }

}
