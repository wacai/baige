package com.wacai.open.baige.common.protocol.header;

public class ResumePullMsgResponseHeader implements CommandCustomHeader {

  private boolean resumePullMsgSuccess;


  public boolean isResumePullMsgSuccess() {
    return resumePullMsgSuccess;
  }

  public void setResumePullMsgSuccess(boolean resumePullMsgSuccess) {
    this.resumePullMsgSuccess = resumePullMsgSuccess;
  }

  @Override
  public void checkFields() {

  }
}
