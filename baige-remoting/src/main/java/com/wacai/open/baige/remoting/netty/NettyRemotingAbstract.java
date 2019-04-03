package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.common.protocol.RemotingSysResponseCode;
import com.wacai.open.baige.remoting.ChannelEventListener;
import com.wacai.open.baige.remoting.InvokeCallback;
import com.wacai.open.baige.remoting.common.Pair;
import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.remoting.common.SemaphoreReleaseOnce;
import com.wacai.open.baige.remoting.common.ServiceThread;
import com.wacai.open.baige.remoting.exception.RemotingSendRequestException;
import com.wacai.open.baige.remoting.exception.RemotingTimeoutException;
import com.wacai.open.baige.remoting.exception.RemotingTooMuchRequestException;
import com.wacai.open.baige.remoting.listener.RemotingClientListener;
import com.wacai.open.baige.remoting.netty.NettyRequestProcessor.AsyncProcessCallback;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

public abstract  class NettyRemotingAbstract  {


  private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingAbstract.class);

  protected RemotingClientListener remotingClientListener;

  protected final Semaphore semaphoreOneway;

  protected final Semaphore semaphoreAsync;

  protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

  /*关联request code和对应的请求处理器*/
  protected final HashMap<Short/*request code */, Pair<NettyRequestProcessor, ExecutorService>>
   processorTable =  new HashMap<>(64);

  protected final NettyEventExecuter nettyEventExecuter = new NettyEventExecuter();

  protected final ConcurrentHashMap<Integer /* opaque */, ResponseFuture> responseTable =
      new ConcurrentHashMap<Integer, ResponseFuture>(256);

  protected abstract ChannelEventListener getChannelEventListener();


  abstract public ExecutorService getCallbackExecutor();


  public void putNettyEvent(NettyEvent nettyEvent) {
    this.nettyEventExecuter.putNettyEvent(nettyEvent);
  }

  public void scanResponseTable() {

    Iterator<Entry<Integer/*opaque*/, ResponseFuture>> iterator =
        responseTable.entrySet().iterator();
     Entry<Integer,ResponseFuture> entry = null;
    ResponseFuture responseFuture = null;
    while (iterator.hasNext()) {
      entry = iterator.next();
      responseFuture = entry.getValue();
      if ( (responseFuture.getBeginTimeStamp() + responseFuture.getTimeoutMills() + 1000) <=
          System.currentTimeMillis()) { //已到请求超时时间。
        iterator.remove();
        try {
          responseFuture.executeInvokeCallback();
        } catch (Throwable e) {
          LOGGER.warn("scanResponseTable, executeInvokeCallback catch Exception", e);
        } finally {
          responseFuture.release();
        }
      }
    }
  }


  class NettyEventExecuter extends ServiceThread {
    private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue<NettyEvent>();
    private final int MaxSize = 10000;


    public void putNettyEvent(final NettyEvent event) {
      if (this.eventQueue.size() <= MaxSize) {
        this.eventQueue.add(event);
      }
      else {
        LOGGER.warn("event queue of NettyRemotingAbstract#NettyEventExecuter size[{}] enough, so drop this event {}",
            this.eventQueue.size(),
            event.toString());
      }
    }


    @Override
    protected void doLoopService() {

      final ChannelEventListener listener = NettyRemotingAbstract.this.getChannelEventListener();

      try {
        NettyEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
        if (event != null && listener != null) {
          switch (event.getType()) {
            case IDLE:
              listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
              break;
            case CLOSE:
              listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
              break;
            case CONNECT:
              listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
              break;
            case REGISTER:
              listener.onRegister(event.getRemoteAddr(), event.getChannel());
              break;
            case EXCEPTION:
              listener.onChannelException(event.getRemoteAddr(), event.getChannel());
              break;
            default:
              break;

          }
        }
      } catch (Exception e) {
        LOGGER.warn(this.getServiceName() + " service has exception. ", e);
      }

    }


    @Override
    public String getServiceName() {
      return NettyEventExecuter.class.getSimpleName();
    }
  }


  /**
   * 允许单向调用和异步调用的请求信号量。
   * @param permitsOneway oneway调用的信号量值
   * @param permitsAsync  async调用的信号量值
   */
  public NettyRemotingAbstract(final int permitsOneway, final int permitsAsync) {
    this.semaphoreOneway = new Semaphore(permitsOneway, true);
    this.semaphoreAsync = new Semaphore(permitsAsync, true);
  }


  public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
    final RemotingCommand cmd = msg;
    if (cmd != null) {
      switch (cmd.getType()) {
        case REQUEST_COMMAND:
          processRequestCommand(ctx, cmd);
          break;
        case RESPONSE_COMMAND:
          processResponseCommand(ctx, cmd);
          break;
        default:
          break;
      }
    }
  }


  public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {

    final ResponseFuture responseFuture = responseTable.get(cmd.getOpaque());
    if (responseFuture != null) {
      responseFuture.setResponseCommand(cmd);
      responseFuture.release();
      responseTable.remove(cmd.getOpaque());
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
        responseFuture.putResponse(cmd);
      }

    } else {
      LOGGER.warn("receive responseCommand: " + cmd.toString() +  " with opaque:" + cmd.getOpaque()
          + ", but not match any request, remote addr: "
          + RemotingUtil.parseChannelRemoteAddr(ctx.channel()));

    }
  }

  public void processRequestCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {
    final Pair<NettyRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.getCode());
    final Pair<NettyRequestProcessor, ExecutorService> pair = (null == matched) ?
        this.defaultRequestProcessor : matched;
    if (pair != null) {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          pair.getObj1().asyncProcessRequest(ctx, cmd, new AsyncProcessCallback() {
            @Override
            public void onCompleteProcess(RemotingCommand response) {
              if (!cmd.isOnewayRPC()) {
                if (response != null) {
                  response.setOpaque(cmd.getOpaque());
                  response.markResponseType();
                  try {
                    ctx.writeAndFlush(response);
                  } catch (Throwable e) {
                    LOGGER.error("process request over, but write response failed , requestCmd:{}, responseCmd:{}",
                        cmd,
                        response,
                        e);
                  }
                }
              }
            }

            @Override
            public void onException(Throwable e) {
              LOGGER.error("process request catch Exception, cmd is {} ", cmd, e);
              if (!cmd.isOnewayRPC()) {
                final RemotingCommand response = RemotingCommand.createResponseCommand(
                    RemotingSysResponseCode.SYSTEM_ERROR,
                    RemotingUtil.exceptionSimpleDesc(e));

                ctx.writeAndFlush(response);
              }
            }
          });
        }
      };

      try {
        pair.getObj2().submit(runnable);
      } catch (RejectedExecutionException e) {
        if (System.currentTimeMillis() % 1000 == 0) {
          String remoteAddr = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
          LOGGER.warn(remoteAddr + " has too many requests and system thread pool busy, request code:"
           + cmd.getCode(), e);
        }
        if (!cmd.isOnewayRPC()) {
          final RemotingCommand response = RemotingCommand.createResponseCommand(
              RemotingSysResponseCode.SYSTEM_BUSY, " too many requests and system thread pool busy");
          response.setOpaque(cmd.getOpaque());
          ctx.writeAndFlush(response);
        }
      }
    } else {
      //未找到匹配request code的处理器。
      String error = "request type " + cmd.getCode() + " is not supported";
      final RemotingCommand response = RemotingCommand.createResponseCommand(
          RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, error);
      response.setOpaque(cmd.getOpaque());
      ctx.writeAndFlush(response);
      LOGGER.error(error + ", the remote addr:" + RemotingUtil.parseChannelRemoteAddr(ctx.channel()));

    }
  }

  public void invokeAysncImpl(Channel channel, RemotingCommand request, long timeoutMills, InvokeCallback invokeCallback)
      throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {

    /*通过信号量控制异步调用的并发量*/
    boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMills, TimeUnit.MILLISECONDS);
    if (acquired) {
      final SemaphoreReleaseOnce once = new SemaphoreReleaseOnce(this.semaphoreAsync);
      final ResponseFuture responseFuture = new ResponseFuture(request.getOpaque(),
          timeoutMills, invokeCallback, once);
      this.responseTable.put(request.getOpaque(), responseFuture);
      try {
        /*在ChannelFuture中区分了请求发送成功和失败两种情况。*/
        channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
              responseFuture.setSendReqOK(true);
              return;
            } else {
              responseFuture.setSendReqOK(false);
            }
            /*请求发送失败*/
            responseFuture.putResponse(null);
            responseTable.remove(request.getOpaque());
            try {
              responseFuture.executeInvokeCallback();
            } catch (Throwable e) {
              LOGGER.warn("excute callback in writeAndFlush addListener, and callback throw", e);
            } finally {
              responseFuture.release();
            }

            LOGGER.warn("send a request command to channel <{}> failed.",
                RemotingUtil.parseChannelRemoteAddr(channel));
            LOGGER.warn(request.toString());
          }
        });
      } catch (Exception e) {
        responseFuture.release();
        String remoteAddr = RemotingUtil.parseChannelRemoteAddr(channel);
        LOGGER.warn(
            "send a request command to channel <" + remoteAddr
                + "> Exception", e);
        throw new RemotingSendRequestException(remoteAddr, e);
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

  public  RemotingCommand invokeSyncImpl(Channel channel, RemotingCommand request, long timeoutMills)
      throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
    try {
      final ResponseFuture responseFuture =
          new ResponseFuture(request.getOpaque(), timeoutMills, null, null);
      this.responseTable.put(request.getOpaque(), responseFuture);
      channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture f) throws Exception {
          if (f.isSuccess()) {
            responseFuture.setSendReqOK(true);
            return;
          }
          else {
            responseFuture.setSendReqOK(false);
          }
          /*请求发送失败。*/
          responseTable.remove(request.getOpaque());
          responseFuture.setCause(f.cause());
          responseFuture.putResponse(null);
          LOGGER.warn("send a request command to channel <" + channel.remoteAddress() + "> failed.");
          LOGGER.warn(request.toString());
        }
      });
      /*阻塞等待调用结果*/
      RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMills);
      if (null == responseCommand) {
        if (responseFuture.isSendReqOK()) {
          throw new RemotingTimeoutException(RemotingUtil.parseChannelRemoteAddr(channel),
              timeoutMills, responseFuture.getCause());
        } else {
          throw new RemotingSendRequestException(RemotingUtil.parseChannelRemoteAddr(channel),
              responseFuture.getCause());
        }
      }
      return responseCommand;
    } finally {
      this.responseTable.remove(request.getOpaque());
    }
  }







}
