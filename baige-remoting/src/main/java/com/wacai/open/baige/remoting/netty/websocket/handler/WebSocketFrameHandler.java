package com.wacai.open.baige.remoting.netty.websocket.handler;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import java.util.List;

/**
 * 负责BinaryWebSocketFrame 和 ByteBuf的编解码
 * decode:  把BinaryWebSocketFrame 解码成 ByteBuf
 * encode:  把ByteBuf 编码成 BinaryWebSocketFrame
 */
public  class WebSocketFrameHandler extends MessageToMessageCodec<BinaryWebSocketFrame, ByteBuf> {

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
      throws Exception {
      /*把ByteBuf编码成BinaryWebSocketFrame*/
    BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(msg);
    out.add(binaryWebSocketFrame.retain());
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out)
      throws Exception {
      /*把BinaryWebSocketFrame 解析成 ByteBuf*/
    BinaryWebSocketFrame binaryWebSocketFrame = msg.retain();
    out.add(binaryWebSocketFrame.content());

  }
}