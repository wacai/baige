package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.remoting.ChannelEventListener;
import com.wacai.open.baige.remoting.InvokeCallback;
import com.wacai.open.baige.remoting.RemotingServer;
import com.wacai.open.baige.remoting.common.Pair;
import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.remoting.exception.RemotingSendRequestException;
import com.wacai.open.baige.remoting.exception.RemotingTimeoutException;
import com.wacai.open.baige.remoting.exception.RemotingTooMuchRequestException;
import com.wacai.open.baige.remoting.netty.handler.NettyConnetManageHandler;
import com.wacai.open.baige.remoting.netty.handler.NettyServerHandler;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingServer.class);


  private final NettyServerConfig nettyServerConfig;
  private final ChannelEventListener channelEventListener;
  /*服务器启动的辅助类*/
  private final ServerBootstrap serverBootStrap;
  /*response code处理的公用线程池*/
  private final ExecutorService publicExecutor;

  /*服务器套接字通道的事件轮询*/
  private final EventLoopGroup eventLoopGroupBoss;
  /*套接字通道的事件轮询*/
  private final EventLoopGroup eventLoopGroupSelector;
  /*服务器工作线程池*/
  private DefaultEventExecutorGroup defaultEventExecutorGroup;
  /*异步请求的response的超时处理器。 */
  private final ScheduledExecutorService scheduledExecutorService;

  private int port;


  public NettyRemotingServer(NettyServerConfig nettyServerConfig) {
    this(nettyServerConfig, null);
  }

  public NettyRemotingServer(NettyServerConfig nettyServerConfig,
      ChannelEventListener channelEventListener) {
    super(nettyServerConfig.getServerOnewaySemaphoreValue(), nettyServerConfig.getServerAsyncSemaphoreValue());
    this.serverBootStrap = new ServerBootstrap();
    this.nettyServerConfig = nettyServerConfig;
    this.channelEventListener = channelEventListener;

    //用于request code 处理的公用线程池的线程数。
    int publicThreadNums = nettyServerConfig.getServerCallbackExecutorThreads();
    if (publicThreadNums <= 0) {
      publicThreadNums = 32;
    }
    //用于 request code 处理的公用线程池。
    this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactory() {
      private AtomicInteger threadIndex = new AtomicInteger(0);

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "NettyServerPublicExecutor_" + this.threadIndex.incrementAndGet());
      }
    });

    //serversocket channel就绪事件轮询。
    this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
      private AtomicInteger threadIndex = new AtomicInteger(0);

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, String.format("NettyBoss_%d", this.threadIndex.incrementAndGet()));
      }
    });

    if (RemotingUtil.isLinuxPlatform() && nettyServerConfig.isUseEpollNativeSelector()) {
      this.eventLoopGroupSelector = new EpollEventLoopGroup(
          nettyServerConfig.getServerSelectorThreads(),
          new ThreadFactory() {
            private int threadTotal = nettyServerConfig.getServerSelectorThreads();
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
              return new Thread(r, String.format("NettyServerEPOLLSelector_%d_%d", threadTotal, threadIndex.incrementAndGet()));
            }
          });
    } else {
      this.eventLoopGroupSelector = new NioEventLoopGroup(
          nettyServerConfig.getServerSelectorThreads(),
          new ThreadFactory() {
            private int threadTotal = nettyServerConfig.getServerSelectorThreads();
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
              return new Thread(r, String.format("NettyServerNIOSelector_%d_%d", threadTotal, threadIndex.incrementAndGet()));
            }
          });
    }

    this.scheduledExecutorService =  Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
        "NettyRemotingServerAsyncResponseScanScheduledThread"));


  }

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
  public RemotingCommand invokeSync(Channel channel, RemotingCommand request,
      long timeoutMillis)
      throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
    return this.invokeSyncImpl(channel, request, timeoutMillis);

  }

  @Override
  public void invokeAsync(Channel channel, RemotingCommand request,
      long timeoutMillis, InvokeCallback invokeCallback)
      throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {

     this.invokeAysncImpl(channel, request, timeoutMillis, invokeCallback);
  }

  /**
   * 在子类中可以进行复写，请参考WebSocketRemotingServer}
   * @return
   */
  protected  ChannelInitializer<SocketChannel> createChannelInitializer(EventExecutorGroup group
   ,NettyRemotingServer nettyRemotingServer) {
    return new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
            group,
            new NettyEncoder(),
            new NettyDecoder(),
            new IdleStateHandler(0, 0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()),
            new NettyConnetManageHandler(nettyRemotingServer),
            new NettyServerHandler(nettyRemotingServer)
        );
      }
    };
  }

  @Override
  public void start() {
    this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
        nettyServerConfig.getServerWorkerThreads(),
        new ThreadFactory() {
          public AtomicInteger threadIndex = new AtomicInteger(0);

          @Override
          public Thread newThread(Runnable r) {
            return new Thread(r, "NettyServerCodecThread_" + this.threadIndex.incrementAndGet());
          }
        });

    ServerBootstrap serverBootstrap =
        this.serverBootStrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_KEEPALIVE, false)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize())
            .option(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize())
            .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
            .childHandler(this.createChannelInitializer(defaultEventExecutorGroup, this));

    if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
      if (nettyServerConfig.isDirectBufferPreferred()) {
        LOGGER.warn("the NettyRemotingServer use PooledByteBufAllocator(preferDirect:true)");
        serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
      } else {
        LOGGER.warn("the NettyRemotingServer use PooledByteBufAllocator(preferDirect:false)");
        serverBootstrap.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false));
      }
    }
    InetSocketAddress addr = null;
    try {
      ChannelFuture channelFuture = this.serverBootStrap.bind().sync();
      addr = (InetSocketAddress) channelFuture.channel().localAddress();
      this.port = addr.getPort();
    } catch (InterruptedException e) {
      throw new RuntimeException("ServerBootstrap.bind().sync() catch InterruptedException", e);
    }

    if (channelEventListener != null) {
      this.nettyEventExecuter.start();
    }

    /*异步请求的response的timeout处理*/
    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          NettyRemotingServer.this.scanResponseTable();
        } catch (Exception e) {
          LOGGER.error("NettyRemotingServer scan response table catch Exception", e);
        }
      }
    }, 3000, 1000, TimeUnit.MILLISECONDS);

    LOGGER.info("NettyRemotingServer started on host:{} port:{}", addr.getHostName(), addr.getPort());


  }

  @Override
  public void shutdown() {
    //不接收新的socket 请求
    if (this.eventLoopGroupSelector != null) {
      try {
        this.eventLoopGroupSelector.shutdownGracefully().sync();
      } catch (InterruptedException e) {
        LOGGER.warn("shutdown eventLoopGroupSelector of NettyRemotingServer catch Exception", e);
      }
    }
    if (this.eventLoopGroupBoss != null) {
      try {
        this.eventLoopGroupBoss.shutdownGracefully().sync();
      } catch (InterruptedException e) {
        LOGGER.warn("shutdown eventLoopGroupBoss of NettyRemotingServer catch Exception", e);
      }
    }
    //处理完已经接收进来的socket event再关闭;
    if (defaultEventExecutorGroup != null) {
      try {
        this.defaultEventExecutorGroup.shutdownGracefully().sync();
      } catch (InterruptedException e) {
        LOGGER.warn("shutdown  defaultEventExecutorGroup of NettyRemotingServer catch Exception ", e);
      }
    }

    //关闭掉对response code的处理。
    if (this.publicExecutor != null) {
      this.publicExecutor.shutdown();
    }

    //关闭掉超时Response的处理
    if (this.scheduledExecutorService != null) {
      this.scheduledExecutorService.shutdown();
    }

    if (this.nettyEventExecuter != null) {
      this.nettyEventExecuter.shutdown();
    }


  }

  @Override
  public ChannelEventListener getChannelEventListener() {
    return this.channelEventListener;
  }

  @Override
  public ExecutorService getCallbackExecutor() {
    return this.publicExecutor;
  }




}
