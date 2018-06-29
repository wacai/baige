package com.wacai.open.baige.remoting.netty.handler;


import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.remoting.netty.NettyEvent;
import com.wacai.open.baige.remoting.netty.NettyEventType;
import com.wacai.open.baige.remoting.netty.NettyRemotingServer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyConnetManageHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingServer.class);
  private final NettyRemotingServer nettyRemotingServer;

  public NettyConnetManageHandler(NettyRemotingServer nettyRemotingServer) {
    this.nettyRemotingServer = nettyRemotingServer; 
  }
  
  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
//    LOGGER.info("NETTY SERVER PIPELINE: channelRegistered {}", remoteAddress);
    super.channelRegistered(ctx);

    if (this.nettyRemotingServer.getChannelEventListener() != null) {
      this.nettyRemotingServer.putNettyEvent(new NettyEvent(NettyEventType.REGISTER, remoteAddress.toString(), ctx.channel()));
    }

  }


  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
//    LOGGER.info("NETTY SERVER PIPELINE: channelUnregistered, the channel[{}]", remoteAddress);
    super.channelUnregistered(ctx);
  }


  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
//    LOGGER.info("NETTY SERVER PIPELINE: channelActive, the channel[{}]", remoteAddress);
    super.channelActive(ctx);

    if (this.nettyRemotingServer.getChannelEventListener() != null) {
      this.nettyRemotingServer.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress.toString(), ctx.channel()));
    }
  }


  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
//    LOGGER.info("NETTY SERVER PIPELINE: channelInactive, the channel[{}]", remoteAddress);
    super.channelInactive(ctx);

    if (this.nettyRemotingServer.getChannelEventListener() != null) {
      this.nettyRemotingServer.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress.toString(), ctx.channel()));
    }
  }


  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      if (event.state().equals(IdleState.ALL_IDLE)) {
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        LOGGER.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
        RemotingUtil.closeChannel(ctx.channel());
        if (this.nettyRemotingServer.getChannelEventListener() != null) {
          this.nettyRemotingServer
              .putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress.toString(), ctx.channel()));
        }
      }
    }

    ctx.fireUserEventTriggered(evt);
  }


  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
    LOGGER.warn("NETTY SERVER PIPELINE: exceptionCaught {}", remoteAddress);
    LOGGER.warn("NETTY SERVER PIPELINE: exceptionCaught exception.", cause);

    if (this.nettyRemotingServer.getChannelEventListener() != null) {
      this.nettyRemotingServer.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress.toString(), ctx.channel()));
    }

    RemotingUtil.closeChannel(ctx.channel());
  }
}
