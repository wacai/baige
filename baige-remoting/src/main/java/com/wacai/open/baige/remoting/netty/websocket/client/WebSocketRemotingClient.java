package com.wacai.open.baige.remoting.netty.websocket.client;


import static com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.HttpConstants.Headers.AUTH_INFO;
import static com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.HttpConstants.Headers.SERVER_ID;

import com.alibaba.fastjson.JSON;
import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.common.protocol.RemotingSysResponseCode;
import com.wacai.open.baige.common.protocol.authorize.AuthorizeData;
import com.wacai.open.baige.remoting.ChannelEventListener;
import com.wacai.open.baige.remoting.InvokeCallback;
import com.wacai.open.baige.remoting.RemotingClient;
import com.wacai.open.baige.remoting.common.Pair;
import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.remoting.common.SemaphoreReleaseOnce;
import com.wacai.open.baige.remoting.exception.RemotingConnectException;
import com.wacai.open.baige.remoting.exception.RemotingSendRequestException;
import com.wacai.open.baige.remoting.exception.RemotingTimeoutException;
import com.wacai.open.baige.remoting.exception.RemotingTooMuchRequestException;
import com.wacai.open.baige.remoting.listener.RemotingClientListener;
import com.wacai.open.baige.remoting.netty.NettyClientConfig;
import com.wacai.open.baige.remoting.netty.NettyRemotingAbstract;
import com.wacai.open.baige.remoting.netty.NettyRequestProcessor;
import com.wacai.open.baige.remoting.netty.ResponseFuture;
import com.wacai.open.baige.remoting.netty.websocket.WsSocket;
import com.wacai.open.baige.remoting.netty.websocket.listener.SocketListener;
import com.wacai.open.baige.remoting.netty.websocket.listener.SocketListenerAdaptor;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 */
public class WebSocketRemotingClient  extends NettyRemotingAbstract implements RemotingClient {


  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketRemotingClient.class);

  private final NettyClientConfig nettyClientConfig;
  private final String wsURI;
  /**
   * 负责处理RequestCommand 以及 ResponseCommand回调函数的处理 。
   *
   */
  private final ExecutorService publicExecutor;
  private final AuthorizeData authorizeData;

  private WebSocketClient webSocketClient;
  private WsSocket wsSocket;

//  private final Timer timer = new Timer("ClientHouseKeepingService", true);
  private  ScheduledExecutorService scheduledExecutorService;

  private SocketListener socketListener = new SocketListenerAdaptorImpl();


  /*是否暂停了定时任务*/
  private boolean isSuspendScheduleTask = false;

  public WebSocketRemotingClient(final NettyClientConfig nettyClientConfig,  final String wsURI
  , AuthorizeData authorizeData) {
    super(nettyClientConfig.getClientOnewaySemaphoreValue(),
        nettyClientConfig.getClientAsyncSemaphoreValue());
    this.nettyClientConfig = nettyClientConfig;


    this.webSocketClient = new WebSocketClient(new SslContextFactory(true));
    this.wsSocket = new WsSocket(this.nettyClientConfig.getWsSocketThreads(), wsURI);
    this.wsURI = wsURI;
    this.authorizeData = authorizeData;

    int publicThreadNums = nettyClientConfig.getClientCallbackExecutorThreads();
    if (publicThreadNums <= 0) {
      publicThreadNums = 16;
    }
    this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactory() {
      private AtomicInteger threadIndex = new AtomicInteger(0);


      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
      }
    });


    this.scheduledExecutorService =  Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
            "WebSocketRemotingClientResponseScanScheduledThread"));

  }


  @Override
  protected ChannelEventListener getChannelEventListener() {
    throw new RuntimeException("not supported");
  }

  /*获取回调函数执行器*/
  @Override
  public ExecutorService getCallbackExecutor() {
    return this.publicExecutor;
  }



  public void processResponseCommand(WsSocket wsSocket,  RemotingCommand responseCommand) {

    final ResponseFuture responseFuture = responseTable.get(responseCommand.getOpaque());
    if (responseFuture != null) {
      responseFuture.setResponseCommand(responseCommand);
      responseFuture.release();
      responseTable.remove(responseCommand.getOpaque());
      if (responseFuture.getInvokeCallback() != null) {
        boolean runInThisThread = false;
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null) {
          try {
            executor.submit(new Runnable() {
              @Override
              public void run() {
                try {
                  responseFuture.executeInvokeCallback();
                } catch (Throwable e) {
                  LOGGER.warn("excute callback in executor exception, and callback throw", e);
                }
              }
            });
          } catch (Exception e) {
            runInThisThread = true;
            LOGGER.warn("excute callback in executor exception, maybe executor busy", e);
          }
        } else {
          runInThisThread = true;
        }
        if (runInThisThread) {
          /*提交到public Executor 失败或者没有public Executor ,则在原线程中执行*/
          try {
            responseFuture.executeInvokeCallback();
          } catch (Throwable e) {
            LOGGER.warn("executeInvokeCallback Exception", e);
          }
        }
      } else {
        //responseFuture不带回调函数。
        responseFuture.putResponse(responseCommand);
      }

    } else {
      LOGGER.warn("receive responseCommand: " + responseCommand.toString() +  " with opaque:" + responseCommand.getOpaque()
          + ", but not match any request, remote wsURI:" + this.wsURI);

    }
  }



  public void processRequestCommand(WsSocket wsSocket,  RemotingCommand cmd) {
    final Pair<NettyRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.getCode());
    final Pair<NettyRequestProcessor, ExecutorService> pair = (null == matched) ?
        this.defaultRequestProcessor : matched;
    if (pair != null) {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          try {
            final RemotingCommand response = pair.getObj1().processRequest(wsSocket, cmd);
            if (!cmd.isOnewayRPC()) {
              if (response != null) {
                response.setOpaque(cmd.getOpaque());
                response.markResponseType();
                try {
//                  ctx.writeAndFlush(response);
                  wsSocket.sendRemotingCommand(response);
                } catch (Throwable e) {
                  LOGGER.error("process request over, but write response failed , requestCmd:{}, responseCmd:{}",
                      cmd,
                      response,
                      e);
                }
              }
            }
          } catch (Throwable e) {
            LOGGER.error("process request catch Exception, cmd is {} ", cmd, e);
            if (!cmd.isOnewayRPC()) {
              final RemotingCommand response = RemotingCommand.createResponseCommand(
                  RemotingSysResponseCode.SYSTEM_ERROR,
                  RemotingUtil.exceptionSimpleDesc(e));

//              ctx.writeAndFlush(response);
              try {
                wsSocket.sendRemotingCommand(response);
              } catch (IOException e1) {
                e1.printStackTrace();
              }
            }

          }
        }
      };

      try {
        pair.getObj2().submit(runnable);
      } catch (RejectedExecutionException e) {
        if (System.currentTimeMillis() % 1000 == 0) {
          /*线程调度请求被拒绝 */
          LOGGER.warn("wsURI"  + this.wsURI + " has too many requests and system thread pool busy, request code:"
              + cmd.getCode(), e);
        }
        if (!cmd.isOnewayRPC()) {
          final RemotingCommand response = RemotingCommand.createResponseCommand(
              RemotingSysResponseCode.SYSTEM_BUSY, " too many requests and system thread pool busy");
          response.setOpaque(cmd.getOpaque());
//          ctx.writeAndFlush(response);
          try {
            wsSocket.sendRemotingCommand(response);
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
    } else {
      //未找到匹配request code的处理器。
      String error = "request type " + cmd.getCode() + " is not supported";
      final RemotingCommand response = RemotingCommand.createResponseCommand(
          RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, error);
      response.setOpaque(cmd.getOpaque());
//      ctx.writeAndFlush(response);
      try {
        wsSocket.sendRemotingCommand(response);
      } catch (IOException e) {
        LOGGER.warn("wsSocket send {} to wsURI catch Exception ", response, this.wsURI, e);
      }
      LOGGER.error(error + ", the wsURI:" + this.wsURI);

    }
  }



  public void processMessageReceived(WsSocket wsSocket, RemotingCommand msg) throws Exception {
    final RemotingCommand cmd = msg;
    if (cmd != null) {
      switch (cmd.getType()) {
        case REQUEST_COMMAND:
          processRequestCommand(wsSocket, cmd);
          break;
        case RESPONSE_COMMAND:
          processResponseCommand(wsSocket, cmd);
          break;
        default:
          break;
      }
    }
  }


  @Override
  public RemotingCommand invokeSync(String addr, RemotingCommand request,
      long timeoutMills)
      throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
    try {
      ResponseFuture responseFuture = new ResponseFuture(request.getOpaque(), timeoutMills, null, null);
      this.responseTable.put(request.getOpaque(), responseFuture);
      Future future = null;
      try {
        future = wsSocket.sendRemotingCommand(request);
      } catch (IOException e) {
        LOGGER.warn("wsSocket send {} to wsURI catch Exception ", request, this.wsURI, e);
        throw new RemotingSendRequestException(this.wsURI, e.getCause());
      }
      /*阻塞等待调用结果*/
      RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMills);
      if (null == responseCommand) {
        if (future.isDone()) {
          throw new RemotingTimeoutException(this.wsURI,
              timeoutMills, responseFuture.getCause());
        } else {
          throw new RemotingSendRequestException(this.wsURI,
              responseFuture.getCause());
        }
      }
      return responseCommand;
    } finally {
      this.responseTable.remove(request.getOpaque());
    }

  }

  /*发起异步调用*/
  @Override
  public void invokeAsync(String addr, RemotingCommand request, long timeoutMills,
      InvokeCallback invokeCallback)
      throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {

    /*通过信号量控制异步调用的并发量*/
    boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMills, TimeUnit.MILLISECONDS);
    if (acquired) {
      final SemaphoreReleaseOnce once = new SemaphoreReleaseOnce(this.semaphoreAsync);
      final ResponseFuture responseFuture = new ResponseFuture(request.getOpaque(),
          timeoutMills, invokeCallback, once);
      this.responseTable.put(request.getOpaque(), responseFuture);
      try {
        wsSocket.sendRemotingCommand(request);
        responseFuture.setSendReqOK(true);
      } catch (Exception e) {
        responseFuture.setSendReqOK(false);
        responseFuture.release();
        LOGGER.warn(
            "send a request command to wsURI:  <" + this.wsURI
                + "> Exception", e);
        throw new RemotingSendRequestException(this.wsURI, e);
      }

    } else {
      if (timeoutMills <= 0) {
        throw new RemotingTooMuchRequestException("invokeAsyncImpl invoke too fast");
      }
      else {
        String info =
            String
                .format(
                    "invokeAsyncImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d", //
                    timeoutMills,//
                    this.semaphoreAsync.getQueueLength(),//
                    this.semaphoreAsync.availablePermits()//
                );
        LOGGER.warn(info);
        LOGGER.warn(request.toString());
        throw new RemotingTimeoutException(info);
      }
    }
  }

  /**
   * 注册server发送给client的请求处理器。
   */
  @Override
  public void registerProcessor(short requestCode, NettyRequestProcessor processor,
      ExecutorService executorService) {
    ExecutorService executorThis = executorService;
    if (null == executorThis) {
      executorThis = this.publicExecutor;
    }
    Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<>(processor, executorThis);
    this.processorTable.put(requestCode, pair);

  }


  @Override
  public void start() throws Exception {
    this.webSocketClient.start();
    URI uri = new URI(this.wsURI);
    ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();

    /*握手时带上授权认证信息*/
    String authorizeData = JSON.toJSONString(this.authorizeData);
    clientUpgradeRequest.setHeader(AUTH_INFO, authorizeData);
    clientUpgradeRequest.setHeader(SERVER_ID, RemotingUtil.getLocalAddress());

    wsSocket.registerSocketListener(socketListener);
    this.webSocketClient.connect(this.wsSocket, uri, clientUpgradeRequest);

  }

  @Override
  public void shutdown() {

    if (this.webSocketClient != null) {
      try {
        this.webSocketClient.stop();
      } catch (Exception e) {
        LOGGER.warn("close websocket client, wsURI: {} catch Exception", this.wsURI, e);
      }
    }


    if (this.publicExecutor != null) {
      try {
        this.publicExecutor.shutdown();
      } catch (Exception e) {
        LOGGER.error("PublicExecutor shutdown exception", e);
      }
    }

    if (this.wsSocket != null) {
      this.wsSocket.close();
    }

    if (this.scheduledExecutorService != null) {
      this.scheduledExecutorService.shutdown();
    }
  }

  @Override
  public void registerListener(RemotingClientListener remotingClientListener) {
    this.remotingClientListener = remotingClientListener;
  }

  @Override
  public RemotingClientListener getRemotingClientListener() {
    return this.remotingClientListener;
  }



  @Override
  public Future<Boolean> reconnect(long connectTimeoutMs) throws Exception {

//    this.webSocketClient.stop();
//    this.webSocketClient.start();
//    this.wsSocket = new WsSocket(this.nettyClientConfig.getWsSocketThreads(), wsURI);

    URI uri = new URI(this.wsURI);
    ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
    String authorizeData = JSON.toJSONString(this.authorizeData);
    clientUpgradeRequest.setHeader(AUTH_INFO, authorizeData);
    clientUpgradeRequest.setHeader(SERVER_ID, RemotingUtil.getLocalAddress());

//    wsSocket.registerSocketListener(socketListener);

    Future<Session> future = this.webSocketClient.connect(this.wsSocket, uri, clientUpgradeRequest);
    Session session = future.get(connectTimeoutMs, TimeUnit.MILLISECONDS);
    if (session != null && session.isOpen()) {
      return new Future<Boolean>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
          return false;
        }

        @Override
        public boolean isCancelled() {
          return false;
        }

        @Override
        public boolean isDone() {
          return true;
        }

        @Override
        public Boolean get() throws InterruptedException, ExecutionException {
          return true;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
          return true;
        }
      };
    } else {
      return new Future<Boolean>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
          return false;
        }

        @Override
        public boolean isCancelled() {
          return false;
        }

        @Override
        public boolean isDone() {
          return true;
        }

        @Override
        public Boolean get() throws InterruptedException, ExecutionException {
          return false;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
          return false;
        }
      };
    }

  }

  @Override
  public void suspend() {
    isSuspendScheduleTask = true;
  }

  @Override
  public void resume() {
    isSuspendScheduleTask = false;
  }

  private class SocketListenerAdaptorImpl extends SocketListenerAdaptor {

    @Override
    public void onRecvCommand(WsSocket wsSocket, RemotingCommand remotingCommand) {

      try {
        processMessageReceived(wsSocket, remotingCommand);
      } catch (Exception e) {
        LOGGER.warn("process command [type:{}] catch Exception",
            remotingCommand.isResponseType() ? "response" : "request",
            e);
      }

    }

    @Override
    public void onWebSocketConnect(WsSocket wsSocket, Session session, int connectTimes) throws InterruptedException {
//      wsSocket.awaitConnect(nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);

      RemotingClientListener remotingClientListener = getRemotingClientListener();
      if (remotingClientListener != null) {
        remotingClientListener.onConnect(wsURI, connectTimes);
      }


      scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
          if (!isSuspendScheduleTask) {
            WebSocketRemotingClient.this.scanResponseTable();
          }
        }
      }, 3000, 2000, TimeUnit.MILLISECONDS);
    }


    @Override
    public void onWebSocketError(WsSocket wsSocket, Throwable error) {

      getRemotingClientListener().onRemotingError(error);
    }

    @Override
    public void onWebSocketClose(WsSocket wsSocket, int statusCode, String reason,
        InetSocketAddress remoteAddr) {
      RemotingClientListener remotingClientListener = getRemotingClientListener();
      if (remotingClientListener != null) {
        remotingClientListener.onClose(remoteAddr);
      }

    }

  }



}
