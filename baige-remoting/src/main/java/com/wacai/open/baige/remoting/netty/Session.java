package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.common.protocol.authorize.AuthorizeData;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 *  标识一个通道的会话信息
 */
public class Session {

  private static final String SESSION_KEY_NAME = "session";

  /*关联的通道*/
  private Channel channel;


  /**
   * 通过认证的认证信息。
   */
  private AuthorizeData authorizeData;


  /*消费者分组*/
  private String consumerGroup;

  /*消费者id */
  private String consumerId;




  public static Session getSession(ChannelHandlerContext channelHandlerContext) {
    if (channelHandlerContext == null || channelHandlerContext.channel() == null) {
      return null;
    }
    AttributeKey<Session> sessionAttributeKey = AttributeKey.valueOf(SESSION_KEY_NAME);
    return channelHandlerContext.channel().attr(sessionAttributeKey).get();
  }

  public static Session getSession(Channel channel) {
    if (channel == null) {
      return null;
    }
    AttributeKey<Session> sessionAttributeKey = AttributeKey.valueOf(SESSION_KEY_NAME);
    return channel.attr(sessionAttributeKey).get();
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;

    AttributeKey<Session> sessionAttributeKey = AttributeKey.valueOf(SESSION_KEY_NAME);
    Attribute<Session> sessionAttribute = channel.attr(sessionAttributeKey);
    sessionAttribute.set(this);
  }

  public AuthorizeData getAuthorizeData() {
    return authorizeData;
  }

  public void setAuthorizeData(AuthorizeData authorizeData) {
    this.authorizeData = authorizeData;
  }


  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  public String getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(String consumerId) {
    this.consumerId = consumerId;
  }
}
