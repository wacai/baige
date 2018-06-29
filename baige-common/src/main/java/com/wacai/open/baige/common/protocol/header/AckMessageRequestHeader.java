package com.wacai.open.baige.common.protocol.header;

public class AckMessageRequestHeader implements CommandCustomHeader {


  private String threadId;
  private long offset;
  private String topic;



  @Override
  public void checkFields() {

  }

  public String getThreadId() {
    return threadId;
  }

  public void setThreadId(String threadId) {
    this.threadId = threadId;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }
}
