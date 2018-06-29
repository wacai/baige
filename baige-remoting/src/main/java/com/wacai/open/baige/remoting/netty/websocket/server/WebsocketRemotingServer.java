package com.wacai.open.baige.remoting.netty.websocket.server;

import com.wacai.open.baige.remoting.ChannelEventListener;
import com.wacai.open.baige.remoting.netty.NettyDecoder;
import com.wacai.open.baige.remoting.netty.NettyEncoder;
import com.wacai.open.baige.remoting.netty.NettyRemotingServer;
import com.wacai.open.baige.remoting.netty.NettyServerConfig;
import com.wacai.open.baige.remoting.netty.handler.NettyConnetManageHandler;
import com.wacai.open.baige.remoting.netty.handler.NettyServerHandler;
import com.wacai.open.baige.remoting.netty.websocket.handler.HttpRequestHandler;
import com.wacai.open.baige.remoting.netty.websocket.handler.HttpRequestHandler.AuthorizeHook;
import com.wacai.open.baige.remoting.netty.websocket.handler.WebSocketFrameHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.EventExecutorGroup;

public class WebsocketRemotingServer extends NettyRemotingServer {

  /**
   * 比如URL是: ws://ip:port/ws
   * 那么path是 /ws
   */
  private final String webSocketPath;

  private final AuthorizeHook authorizeHook;

  public WebsocketRemotingServer(NettyServerConfig nettyServerConfig,
      ChannelEventListener channelEventListener, String webSocketPath, AuthorizeHook authorizeHook) {
    super(nettyServerConfig, channelEventListener);
    this.webSocketPath = webSocketPath;
    this.authorizeHook = authorizeHook;
  }




  @Override
  protected ChannelInitializer<SocketChannel> createChannelInitializer(EventExecutorGroup group
      , NettyRemotingServer nettyRemotingServer) {
    return new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
            group,
            /*
            new NettyEncoder(),
            new NettyDecoder(),
            new IdleStateHandler(0, 0, nettyServerConfig.getSearverChannelMaxIdleTimeSeconds()),
            new NettyConnetManageHandler(nettyRemotingServer),
            new NettyServerHandler()
            */
            new HttpServerCodec(), //request decode and response encode ;
            new HttpObjectAggregator(64 * 1024),//聚合Http请求成为一个FullHttpRequest
            new HttpRequestHandler(webSocketPath, authorizeHook),//匹配path,则升级成websocket请求；否则是Http请求。
            new WebSocketServerProtocolHandler(webSocketPath),//处理websocket握手和frame解析。
            new WebSocketFrameHandler(),//负责BinaryWebSocketFrame和ByteBuf的编解码
            new NettyEncoder(),//RemotingCommand编码成ByteBuf
            new NettyDecoder(),// ByteBuf 解析成RemotingCommand;
            /*不对socket channel的idle事件进行处理 ，所以这里注释掉； 服务端不主动对idle连接进行关闭操作。*/
//              new IdleStateHandler(0, 0, nettyServerConfig.getSearverChannelMaxIdleTimeSeconds()),
            new NettyConnetManageHandler(nettyRemotingServer),
            new NettyServerHandler(nettyRemotingServer) //处理RemotingCommand;
        );
      }
    };
  }
}
