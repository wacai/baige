package com.wacai.open.baige.sdk.producer;

public enum SendStatus {

  /*发送成功*/
  OK,
  /**
   * 发送失败
   */
  FAILURE,
  /**
   * TOPIC 不存在
   */
  TOPIC_NOT_EXIST,

  /*Topic 未授权给应用进行投递。*/
  TOPIC_NOT_AUTHORIZED,

  /*msg body 超过最大限制大小 */

  MSG_BODY_EXCEED_LIMIT;
}
