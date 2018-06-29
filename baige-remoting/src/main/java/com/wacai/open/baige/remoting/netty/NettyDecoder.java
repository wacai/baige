package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyDecoder extends LengthFieldBasedFrameDecoder{

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyDecoder.class);

  /*默认最大frame的长度： 32M */
  private static final int FRAME_MAX_LENGTH = //
      Integer.parseInt(System.getProperty("com.wacai.open.baige.remoting.frameMaxLength",
          "33554432"));


  public  NettyDecoder() {
    /*length(4) | header length(2) | header data | body data
    * ,  此配置摘掉头4个字节 。*/
    super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
  }



  @Override
  public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
    ByteBuf frame = null;
    try {
      frame = (ByteBuf) super.decode(ctx, in);
      if (null == frame) {
        return null;
      }
      ByteBuffer byteBuffer = frame.nioBuffer();
      return RemotingCommand.decode(byteBuffer);
    } catch (Exception e) {
      if (ctx != null) {
        LOGGER.error("NettyDecoder decode catch Exception, remote addr:"
            + RemotingUtil.parseChannelRemoteAddr(ctx.channel()), e);
        RemotingUtil.closeChannel(ctx.channel());
      }
    } finally {
      if (null != frame) {
        frame.release();
      }
    }
    return null;
  }


}
