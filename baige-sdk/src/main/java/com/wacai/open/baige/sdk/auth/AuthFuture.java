package com.wacai.open.baige.sdk.auth;

import com.wacai.open.baige.common.protocol.authorize.AuthResult;
import com.wacai.open.baige.sdk.exception.AuthException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AuthFuture {

  private AuthResult authResult;

  private CountDownLatch countDownLatch;

  public AuthFuture() {
    this.countDownLatch = new CountDownLatch(1);
  }


  public void authComplete(AuthResult authResult) {
    this.authResult = authResult;
    this.countDownLatch.countDown();

  }


  public AuthResult waitAuthResult(long authTimeoutMs) throws InterruptedException, AuthException {
    boolean awaitResult = this.countDownLatch.await(authTimeoutMs, TimeUnit.MILLISECONDS);
    if (!awaitResult) {
      throw new AuthException("wait AuthResult timeout", null);
    } else {
      return authResult;
    }
  }
}
