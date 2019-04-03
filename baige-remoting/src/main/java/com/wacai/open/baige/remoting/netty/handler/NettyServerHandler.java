package com.wacai.open.baige.remoting.netty.handler;


import com.wacai.open.baige.remoting.netty.NettyRemotingServer;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {

  private final NettyRemotingServer nettyRemotingServer;

  public NettyServerHandler(NettyRemotingServer nettyRemotingServer) {
    this.nettyRemotingServer = nettyRemotingServer;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
    this.nettyRemotingServer.processMessageReceived(ctx, msg);
  }
}