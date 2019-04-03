package com.wacai.open.baige.common.protocol.authorize;

import com.wacai.open.baige.common.protocol.RemotingSerializable;

public class AuthResult extends RemotingSerializable {

  public static final AuthResult AUTH_SUCCESS = new AuthResult(true);
  public static final AuthResult AUTH_FAILED = new AuthResult(false, "sign is not correct");


  private boolean success;
  private String failReason;

  public AuthResult(boolean success) {
    this(success, null);
  }

  public AuthResult(boolean success, String failReason) {
    this.success = success;
    this.failReason = failReason;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getFailReason() {
    return failReason;
  }

  public void setFailReason(String failReason) {
    this.failReason = failReason;
  }




  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("AuthResult[");
    s.append("success=").append(this.success).append(",");
    s.append("failReason=").append(this.failReason).append(",");
    s.append("]");
    return s.toString();
  }


}
