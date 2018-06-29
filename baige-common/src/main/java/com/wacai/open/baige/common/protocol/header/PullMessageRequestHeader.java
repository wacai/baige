package com.wacai.open.baige.common.protocol.header;

public class PullMessageRequestHeader implements CommandCustomHeader {

  private String threadId;
  private int maxMsgCount;
  private String topic;



  @Override
  public void checkFields() {

  }

  /*getter and setter methods*/

  public String getThreadId() {
    return threadId;
  }

  public void setThreadId(String threadId) {
    this.threadId = threadId;
  }

  public int getMaxMsgCount() {
    return maxMsgCount;
  }

  public void setMaxMsgCount(int maxMsgCount) {
    this.maxMsgCount = maxMsgCount;
  }


  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }
}
