package com.wacai.open.baige.sdk.exception;

public class ServerException extends Exception {
  private static final long serialVersionUID = 1L;

  private final int responseCode;
  private final String errorMessage;


  public ServerException(String errorMessage, Throwable cause) {
    super(errorMessage,cause);
    this.responseCode = -1;
    this.errorMessage = errorMessage;
  }


  public ServerException(int responseCode, String errorMessage) {
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
