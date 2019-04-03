package com.wacai.open.baige.remoting.netty.websocket.handler;

import static com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.HttpConstants.Headers.AUTH_FAIL_REASON;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.wacai.open.baige.common.exception.AuthorizeException;
import com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.HttpConstants.Headers;
import com.wacai.open.baige.common.protocol.authorize.AuthorizeData;
import com.wacai.open.baige.common.protocol.consume.ConsumeUtil;
import com.wacai.open.baige.remoting.netty.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 匹配path, 则升级成websocket请求；否则是Http请求；
 * 比如URL是: ws://ip:port/ws
 * 那么path是 /ws
 *
 *
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


  private static Logger LOGGER = LoggerFactory.getLogger(HttpRequestHandler.class);

  public interface AuthorizeHook {

    /**
     * 在HTTP 升级成websocket之前被调用，如果返回AuthorizeData 非Null, 则通过授权认证；否则未通过授权
     * @param fullHttpRequest 完整Http请求
     * @return 通过授权认证的信息
     * @throws AuthorizeException  AuthorizeException
     */
    AuthorizeData authroize(FullHttpRequest fullHttpRequest)
        throws AuthorizeException;

    void onSessionOpen(Session session);

  }

  private final String wsUri;
  private AuthorizeHook authorizeHook;

  public HttpRequestHandler(String wsUri) {
    this(wsUri, null);
  }

  public HttpRequestHandler(String wsUri, AuthorizeHook authorizeHook) {
    this.wsUri = wsUri;
    this.authorizeHook = authorizeHook;
  }



  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

    if (wsUri.equalsIgnoreCase(request.uri())) { //匹配websocket的uri
      if (this.authorizeHook != null) {
        AuthorizeData authorizeData = null;
        try {
          authorizeData = this.authorizeHook.authroize(request);
        } catch (AuthorizeException e) {
          DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HTTP_1_1,
              HttpResponseStatus.UNAUTHORIZED);
          fullHttpResponse.headers().set(AUTH_FAIL_REASON, e.getMessage());
          sendHttpResponse(ctx, request, fullHttpResponse);
          return;
        }

        Channel channel = ctx.channel();
        Session session = new Session();
        session.setAuthorizeData(authorizeData);
        session.setChannel(channel);

        LOGGER.info("create Session for channel {}", channel);

        //在建链阶段为sdk自动生成consumergroup和consumerid;
        session.setConsumerGroup(ConsumeUtil.getConsumerGroupName(authorizeData.getAppkey()));
        /*2018-01-23 因为baige-bridge前面加了一层反向代理，所以不能直接用channel的Remote addr作为consumerid*/
        HttpHeaders httpHeaders = request.headers();
        String serverId = httpHeaders.get(Headers.SERVER_ID);
//        String remotingAddr = RemotingUtil.parseChannelRemoteAddr(channel);
        session.setConsumerId(serverId);

        this.authorizeHook.onSessionOpen(session);

      }

      ctx.fireChannelRead(request.retain());

    } else { //非websocket请求，则作为http请求来处理。
      ByteBuf content = Unpooled.copiedBuffer("string", CharsetUtil.UTF_8);
      FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
      res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
      HttpUtil.setContentLength(res, content.readableBytes());

      sendHttpResponse(ctx, request, res);
      return;


    }
  }

  private static void send100Continue(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
        HttpResponseStatus.CONTINUE);
    ctx.writeAndFlush(response);
  }


  private static void sendHttpResponse(
      ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
    // Generate an error page if response getStatus code is not OK (200).
    if (res.status().code() != 200) {
      ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
      res.content().writeBytes(buf);
      buf.release();
      HttpUtil.setContentLength(res, res.content().readableBytes());
    }

    // Send the response and close the connection if necessary.
    ChannelFuture f = ctx.channel().writeAndFlush(res);
    if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
      f.addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
