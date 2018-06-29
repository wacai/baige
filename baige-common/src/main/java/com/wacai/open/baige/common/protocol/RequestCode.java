package com.wacai.open.baige.common.protocol;

public final class RequestCode {

  /*拉消息请求： client-> server*/
  public static final byte PULL_MSG = 1;

  /*心跳请求： client-> server*/
  public static final byte HEART_BEAT = 2;

  /*获取客户端元数据： server-> client */
  public static final byte GET_CLIENT_META_INFO = 3 ;


  /*认证请求: client => server */
  public static final byte AUTHORIZE = 4;


  /*ack消息请求： client-> server */
  public static final byte ACK_MSG = 5;

  /*恢复消息拉取请求： server-> client*/
  public static final byte RESUME_PULL_MSG = 6;

  /*投递消息请求： client-> server*/

  public static final byte PUSH_MSG = 7;
}
