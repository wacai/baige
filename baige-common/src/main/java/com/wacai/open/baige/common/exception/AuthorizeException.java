package com.wacai.open.baige.common.exception;

public class AuthorizeException extends Throwable {

  public AuthorizeException(String errMsg) {
    super(errMsg);
  }

  public AuthorizeException(String errMsg, Throwable t) {
    super(errMsg, t);
  }

  public AuthorizeException(Throwable t) {
    super(t);
  }


}
