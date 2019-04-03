package com.wacai.open.baige.sdk.exception;

public class AckMsgException  extends  Throwable{


  private static final long serialVersionUID = 1L;

  public AckMsgException(String errMsg) {
    super(errMsg);
  }

  public AckMsgException(String errMsg, Throwable t) {
    super(errMsg, t);
  }

  public AckMsgException(Throwable t) {
    super(t);
  }


}
