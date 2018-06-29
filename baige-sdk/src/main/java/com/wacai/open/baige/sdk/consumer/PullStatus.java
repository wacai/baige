package com.wacai.open.baige.sdk.consumer;

public enum  PullStatus {

  FOUND(1,"FOUND"), //
  NO_NEW_MSG(2, "no new msg"),
  NO_MATCHED_MSG(3, "no matched msg"),
  NOT_AUTHORIZED(4, "not authorized"),
  AUTH_SYS_ERROR(5, "auth sys error"),
  TOPIC_NOT_AUTHORIZED(6, "topic not authorized"),
  PULL_NOT_FOUND(7, "no pull msg");

  private int value;
  private String msg;

  PullStatus(int value, String msg) {
    this.value = value;
    this.msg = msg;
  }

  public int getValue() {
    return value;
  }

  public String getMsg() {
    return msg;
  }
}
