package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyEncoder.class);

  @Override
  protected void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out)
      throws Exception {

     try {
       ByteBuffer header = remotingCommand.encodeHeader();
       out.writeBytes(header);
       byte []body = remotingCommand.getBody();
       if (body != null) {
         out.writeBytes(body);
       }
     } catch (Exception e) {
       LOGGER.error("NettyEncoder encode catch Exception, Remote Addr: " +
           RemotingUtil.parseChannelRemoteAddr(ctx.channel()), e);
       if (remotingCommand != null) {
         LOGGER.error(",remoting command:" + remotingCommand.toString());
       }
       RemotingUtil.closeChannel(ctx.channel());

     }
  }
}
