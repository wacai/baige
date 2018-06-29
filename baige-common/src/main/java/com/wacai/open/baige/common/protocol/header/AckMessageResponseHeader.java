package com.wacai.open.baige.common.protocol.header;

public class AckMessageResponseHeader implements CommandCustomHeader {

  private long offset;
  private boolean ackSuccess;
  private String ackFailReason;



  @Override
  public void checkFields() {

  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public boolean isAckSuccess() {
    return ackSuccess;
  }

  public void setAckSuccess(boolean ackSuccess) {
    this.ackSuccess = ackSuccess;
  }

  public String getAckFailReason() {
    return ackFailReason;
  }

  public void setAckFailReason(String ackFailReason) {
    this.ackFailReason = ackFailReason;
  }
}
