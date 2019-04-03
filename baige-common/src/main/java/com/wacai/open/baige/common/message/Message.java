package com.wacai.open.baige.common.message;


public class Message {


  private byte[] payload;

  private long offset;

  private String msgKey;


  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public String getMsgKey() {
    return msgKey;
  }

  public void setMsgKey(String msgKey) {
    this.msgKey = msgKey;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("Message[");
    if (this.payload != null) {
      s.append(",payloadSize=").append(this.payload.length);
    }
    s.append(",offset=").append(this.offset);
    if (this.msgKey != null) {
      s.append(",msgKey=").append(this.msgKey);
    }
    s.append("]");
    return s.toString();
  }
}
