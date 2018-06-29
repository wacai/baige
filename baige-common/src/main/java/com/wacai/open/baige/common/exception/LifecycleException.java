package com.wacai.open.baige.common.exception;

public class LifecycleException extends Exception{

  private static final long serialVersionUID = 1L;

  private final int responseCode;
  private final String errorMessage;


  public LifecycleException(String errorMessage, Throwable cause) {
    super(errorMessage);
    this.responseCode = -1;
    this.errorMessage = errorMessage;
  }


  public LifecycleException(int responseCode, String errorMessage) {
    super(errorMessage);
    this.responseCode = responseCode;
    this.errorMessage = errorMessage;
  }


  public int getResponseCode() {
    return responseCode;
  }


  public String getErrorMessage() {
    return errorMessage;
  }
  
}
