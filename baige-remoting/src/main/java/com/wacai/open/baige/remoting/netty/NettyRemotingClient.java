package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.remoting.ChannelEventListener;
import com.wacai.open.baige.remoting.InvokeCallback;
import com.wacai.open.baige.remoting.RemotingClient;
import com.wacai.open.baige.remoting.common.Pair;
import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.remoting.exception.RemotingConnectException;
import com.wacai.open.baige.remoting.exception.RemotingSendRequestException;
import com.wacai.open.baige.remoting.exception.RemotingTimeoutException;
import com.wacai.open.baige.remoting.exception.RemotingTooMuchRequestException;
import com.wacai.open.baige.remoting.listener.RemotingClientListener;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.Timer;

public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {


  private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingClient.class);

  private static final long LOCK_TIMEOUT_MILLS = 3000;

  private final ChannelEventListener channelEventListener;

  /**
   * 负责处理RequestCommand 以及 ResponseCommand回调函数的处理 。
   *
   */
  private final ExecutorService publicExecutor;
  /* 负责轮询套接字就绪事件*/
  private final EventLoopGroup eventLoopGroupWorker;
  private final Bootstrap bootstrap = new Bootstrap();
  private final NettyClientConfig nettyClientConfig;
  private DefaultEventExecutorGroup defaultEventExecutorGroup;

  private Lock lockChannelTables = new ReentrantLock();
  private final ConcurrentHashMap<String /*server addr*/, ChannelWrapper> channelTables =
      new ConcurrentHashMap<>();

//  private final Timer timer = new Timer("ClientHouseKeepingService", true);

  @Override
  protected ChannelEventListener getChannelEventListener() {
    return this.channelEventListener;
  }

  @Override
  public ExecutorService getCallbackExecutor() {
    return this.publicExecutor;
  }


  class ChannelWrapper {
    private final ChannelFuture channelFuture;

    public ChannelWrapper(ChannelFuture channelFuture) {
      this.channelFuture = channelFuture;
    }

    public boolean isOk() {
      return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
    }

    public boolean isWritable() {
      return this.channelFuture.channel().isWritable();
    }

    private Channel getChannel() {
      return this.channelFuture.channel();
    }

    public ChannelFuture getChannelFuture() {
      return this.channelFuture;
    }

  }

  class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
      processMessageReceived(ctx, msg);
    }
  }

  class NettyConnectManageHandler extends ChannelDuplexHandler {

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
        SocketAddress localAddress, ChannelPromise promise) throws Exception {
      final String local = localAddress == null ? "UNKNOW" : localAddress.toString();
      final String remote = remoteAddress == null ? "UNKNOW" : remoteAddress.toString();
      LOGGER.info("NettyConnectManageHandler#connect   Local:{} => Remote:{}", local, remote);
      super.connect(ctx, remoteAddress, localAddress, promise);

      if (NettyRemotingClient.this.channelEventListener != null) {
        NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress
            .toString(), ctx.channel()));
      }
    }


    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
      LOGGER.info("NettyConnectManageHandler#disconnect  Remote: {}", remoteAddress);
      closeChannel(ctx.channel());
      super.disconnect(ctx, promise);

      if (NettyRemotingClient.this.channelEventListener != null) {
        NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress
            .toString(), ctx.channel()));
      }
    }


    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
      LOGGER.info("NettyConnectManageHandler#close Remote: {}", remoteAddress);
      closeChannel(ctx.channel());
      super.close(ctx, promise);

      if (NettyRemotingClient.this.channelEventListener != null) {
        NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress
            .toString(), ctx.channel()));
      }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
      LOGGER.warn("NettyConnectManageHandler#exceptionCaught, Remote: {}", remoteAddress, cause);
      closeChannel(ctx.channel());
      if (NettyRemotingClient.this.channelEventListener != null) {
        NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress
            .toString(), ctx.channel()));
      }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof IdleStateEvent) {
        IdleStateEvent event = (IdleStateEvent) evt;
        if (event.state().equals(IdleState.ALL_IDLE)) {
          final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
          LOGGER.warn("NettyConnectManageHandler#userEventTriggered IdleStateEvent, Remote: {}",
              remoteAddress);
          closeChannel(ctx.channel());
          if (NettyRemotingClient.this.channelEventListener != null) {
            NettyRemotingClient.this.putNettyEvent(new NettyEvent(NettyEventType.IDLE,
                remoteAddress.toString(), ctx.channel()));
          }
        }
      }

      ctx.fireUserEventTriggered(evt);
    }
  }

  public void closeChannel(Channel channel) {
    if (null == channel) {
      final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(channel);
      try {
        if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLS, TimeUnit.MILLISECONDS)) {
          try {
            boolean removeItemFromTable = true;
            ChannelWrapper prevCW = null;
            String addrRemote = null;
            for (String key : channelTables.keySet()) {
              ChannelWrapper prev = this.channelTables.get(key);
              if (prev.getChannel() != null) {
                if (prev.getChannel() == channel) {  //找到了对应的通道
                  prevCW = prev;
                  addrRemote = key;
                  break;
                }
              }
            }
            if (prevCW == null) {
              LOGGER.info("NettyRemotingClient#closeChannel the channel [Remote:{}] has been removed from channel table before",
                  addrRemote);
              removeItemFromTable = false;
            }
            if (removeItemFromTable) {
              this.channelTables.remove(addrRemote);
              LOGGER.info("NettyRemotingClient#closeChannel the channel [Remote:{}] was removed from channel table",
                  addrRemote);
              RemotingUtil.closeChannel(channel);
            }
          } catch (Exception e) {
            LOGGER.error("close channel remote {} catch Exception ", remoteAddress);
          } finally {
            this.lockChannelTables.unlock();
          }
        } else {
          LOGGER.warn("closeChannel: try to lock channel table, but timeout {} ms", LOCK_TIMEOUT_MILLS);
        }
      } catch (InterruptedException e) {

        LOGGER.error("close channel remote {} catch exception", remoteAddress, e);
      }

    }
  }

  public void closeChannel(final String addr, final Channel channel) {
    if (null == channel) {
      return;
    }
    final String addrRemote = (null == addr) ? RemotingUtil.parseChannelRemoteAddr(channel) : addr;
    try {
      if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLS, TimeUnit.MILLISECONDS)) {
        try {
          boolean removeItemFromTable = true;
          final ChannelWrapper preCW = this.channelTables.get(addrRemote);
          if (null == preCW) { //通道已经被移除了
            LOGGER.info("closeChannel [remoteAddr:{}] has been removed from the channel table before",
                addrRemote);
            removeItemFromTable = false;
          } else if (preCW.getChannel() != channel) {
            //通道被移除，但是套接字被重新创建了。
            LOGGER.info("closeChannel [remoteAddr:{}] has been closed before and has been created again",
                addrRemote);
            removeItemFromTable = false;
          }
          if (removeItemFromTable) {
            this.channelTables.remove(addrRemote);
            LOGGER.info("closeChannel [remoteAddr:{}] was removed from channel table", addrRemote);
          }
          RemotingUtil.closeChannel(channel);
        } catch (Exception e) {
          LOGGER.warn("closeChannel [remoteAddr:{}] catch Exception", addrRemote, e);
        } finally {
          this.lockChannelTables.unlock();
        }
      } else {
        LOGGER.warn("closeChannel: try to lock channel table, but timeout {} ms", LOCK_TIMEOUT_MILLS);
      }
    } catch (InterruptedException e) {
      LOGGER.error("close channel [remote addr:{}] catch Exception ", addrRemote, e);
    }

  }

  public NettyRemotingClient(final NettyClientConfig nettyClientConfig, final
  ChannelEventListener channelEventListener) {
    super(nettyClientConfig.getClientOnewaySemaphoreValue(),
        nettyClientConfig.getClientAsyncSemaphoreValue());
    this.nettyClientConfig = nettyClientConfig;
    this.channelEventListener = channelEventListener;

    int publicThreadNums = nettyClientConfig.getClientCallbackExecutorThreads();
    if (publicThreadNums <= 0) {
      publicThreadNums = 4;
    }


    this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactory() {
      private AtomicInteger threadIndex = new AtomicInteger(0);


      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
      }
    });

    /*单线程轮询套接字就绪事件*/
    this.eventLoopGroupWorker = new NioEventLoopGroup(1, new ThreadFactory() {
      private AtomicInteger threadIndex = new AtomicInteger(0);


      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, String.format("NettyClientSelector_%d",
            this.threadIndex.incrementAndGet()));
      }
    });


  }
//  @Override
//  public RemotingCommand invokeSync(final String addr, RemotingCommand request,
//      long timeoutMills)
//      throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
//    return null;
//  }

  @Override
  public RemotingCommand invokeSync(String addr, RemotingCommand request,
      long timeoutMills)
      throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {

    final Channel channel = this.getOrCreateChannel(addr);
    if (channel != null && channel.isActive()) {
      try {
        RemotingCommand response = this.invokeSyncImpl(channel, request, timeoutMills);
        return response;
      } catch (RemotingSendRequestException e) {
        LOGGER.warn("invokeSync: send request exception, so close the channel[{}]", addr);
        this.closeChannel(addr, channel);
        throw e;
      } catch (RemotingTimeoutException e) {
        LOGGER.warn("invokeSync: wait response timeout exception, the channel[{}]", addr);
        throw e;
      }
    } else {
      this.closeChannel(channel);
      throw new RemotingConnectException(addr);
    }

  }



  @Override
  public void invokeAsync(final String addr, RemotingCommand request, long timeoutMills, InvokeCallback invokeCallback)
      throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {

    final Channel channel = this.getOrCreateChannel(addr);
    if (channel != null && channel.isActive()) {
      if (!channel.isWritable()) {
        throw new RemotingTooMuchRequestException(String.format("the channel [%s] is not writable now", channel.toString()));
      }

      try {
        this.invokeAysncImpl(channel, request, timeoutMills, invokeCallback);
      } catch (RemotingSendRequestException e) {
        LOGGER.warn("invokeAsync: send request exception, so close the channel[{}]", addr);
        this.closeChannel(addr, channel);
        throw e;
      }
    } else {
      this.closeChannel(channel);
      throw new RemotingConnectException(addr);
    }

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
  public void start() {

    this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(//
        nettyClientConfig.getClientWorkerThreads(), //
        new ThreadFactory() {

          private AtomicInteger threadIndex = new AtomicInteger(0);


          @Override
          public Thread newThread(Runnable r) {
            return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
          }
        });

    /*eventLoopGroupWorker 是单线程轮询socket就绪事件； 而处理socket 就绪事件的
    * defaultEventExecutorGroup 默认是4个线程*/
    Bootstrap bootstrap = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_KEEPALIVE, false)
        .option(ChannelOption.SO_SNDBUF, this.nettyClientConfig.getClientSocketSndBufSize())
        .option(ChannelOption.SO_RCVBUF, this.nettyClientConfig.getClientSocketRcvBufSize())
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(
                defaultEventExecutorGroup,
                new NettyEncoder(),
                new NettyDecoder(),
                new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()),
                new NettyConnectManageHandler(),
                new NettyClientHandler()
            );
          }
        });

    /*延迟3秒，执行一秒一次的任务：扫描response table, 通过回调函数处理response*/
//    this.timer.scheduleAtFixedRate(new TimerTask() {
//      @Override
//      public void run() {
//        try {
//          NettyRemotingClient.this.scanResponseTable();
//        } catch (Exception e) {
//          LOGGER.error("scan response table catch exception", e);
//        }
//      }
//    }, 1000 * 3, 1000);

    if (this.channelEventListener != null) {
      this.nettyEventExecuter.start();
    }




  }

  @Override
  public void shutdown() {
    try {
//      this.timer.cancel();

      for (ChannelWrapper channelWrapper : this.channelTables.values()) {
        this.closeChannel(null, channelWrapper.getChannel());
      }

      this.channelTables.clear();
      /*负责套接字就绪事件的轮询*/
      this.eventLoopGroupWorker.shutdownGracefully().sync();

      /**
       * 在父类{@link NettyRemotingAbstract} 中调用{@link ChannelEventListener}
       */
      if (this.nettyEventExecuter != null) {
        this.nettyEventExecuter.shutdown();
      }
      /**
       * 负责套接字就绪事件的处理 。
       */
      if (this.defaultEventExecutorGroup != null) {
        this.defaultEventExecutorGroup.shutdownGracefully().sync();
      }

    } catch (Exception e) {
      LOGGER.error("NettyRemotingClient shutdown catch Exception", e);
    }

    if (this.publicExecutor != null) {
      try {
        this.publicExecutor.shutdown();
      } catch (Exception e) {
        LOGGER.error("PublicExecutor shutdown exception", e);
      }
    }

  }


  private Channel getOrCreateChannel(String addr) throws InterruptedException {
    ChannelWrapper cw = this.channelTables.get(addr);
    if (cw != null && cw.isOk()) {
      return cw.getChannel();
    }
    return this.createChannel(addr);

  }

  private Channel createChannel(String addr) throws InterruptedException {
    ChannelWrapper cw = this.channelTables.get(addr);
    if (cw != null && cw.isOk()) {
      return cw.getChannel();
    }

    if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLS, TimeUnit.MILLISECONDS)) {
      try {
        boolean createNewConn = false;
        cw = this.channelTables.get(addr);
        if (cw != null) {
          if (cw.isOk()) { //通道已经建立。
            return cw.getChannel();
          } else if (!cw.getChannelFuture().isDone()) { //通道还未建立好
            createNewConn = false;
          } else {
            this.channelTables.remove(addr); //移除掉旧通道
            createNewConn = true;
          }
        } else {
          createNewConn = true;
        }
        if (createNewConn) {
          ChannelFuture channelFuture = this.bootstrap.connect(RemotingUtil.string2SocketAddress(addr));
          LOGGER.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
          cw = new ChannelWrapper(channelFuture);
          this.channelTables.put(addr, cw);
        }
      } catch (Exception e) {
        LOGGER.error("createChannel: create channel catch Exception", e);
      } finally {
        this.lockChannelTables.unlock();
      }
    } else {
      LOGGER.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLS);
    }

    if (cw != null) {
      ChannelFuture channelFuture = cw.getChannelFuture();
      if (channelFuture.awaitUninterruptibly(this.nettyClientConfig.getConnectTimeoutMillis())) {
        if (cw.isOk()) {
          LOGGER.info("createChannel: connect remote host[{}] success, {}", addr,
              channelFuture.toString());
          return cw.getChannel();
        }
        else {
          LOGGER.warn(
              "createChannel: connect remote host[" + addr + "] failed, "
                  + channelFuture.toString(), channelFuture.cause());
        }
      } else {
        LOGGER.warn("createChannel: connect remote host[{}] timeout {}ms, {}", addr,
            this.nettyClientConfig.getConnectTimeoutMillis(), channelFuture.toString());
      }
    }

    return null;

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
  public Future<Boolean> reconnect(long connectTimeoutMs) {
    return null;
  }

  @Override
  public void suspend() {

  }

  @Override
  public void resume() {

  }

  @Override
  public void sendKeepaliveHeartBeat(long sendTimeoutMs) {

  }


}
