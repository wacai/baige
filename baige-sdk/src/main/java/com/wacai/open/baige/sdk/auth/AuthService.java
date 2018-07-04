package com.wacai.open.baige.sdk.auth;

import com.wacai.open.baige.common.protocol.authorize.AuthResult;
import com.wacai.open.baige.common.protocol.authorize.AuthorizeData;
import com.wacai.open.baige.common.util.SignUtil;
import com.wacai.open.baige.sdk.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 授权认证服务
 */
public class AuthService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
  private static final char AT_CHAR = '@';

  private final String appSecret;
  private final String appkey;

  private AuthFuture authFuture;


  private volatile boolean isAuthSuccess;


  public AuthService(String appSecret, String appkey) {
    this.authFuture = new AuthFuture();
    this.appSecret = appSecret;
    this.appkey = appkey;

  }



  public AuthorizeData getAuthorizeData() {
    StringBuilder s = new StringBuilder();
    if (this.appkey != null) {
      s.append(this.appkey).append(AT_CHAR);
    }

    String plainText = s.toString();
    String sign = SignUtil.generateSign(plainText, this.appSecret);

    return  new AuthorizeData(this.appkey, plainText, sign);

  }


  /**
   * 等待认证结果
   * @param waitAuthResultTimeoutMs 等待认证结果的超时时间
   * @return AuthResult 认证结果
   * @throws AuthException AuthException
   * @throws InterruptedException InterruptedException
   */
  public AuthResult waitAuthResult(long waitAuthResultTimeoutMs)
      throws AuthException, InterruptedException {

    AuthResult authResult = this.authFuture.waitAuthResult(waitAuthResultTimeoutMs);
    if (authResult != null && authResult.isSuccess()) {
      isAuthSuccess = true;
    } else {
      isAuthSuccess = false;
    }
    return authResult;
  }

  public void authFailed(String failReason) {
    isAuthSuccess = false;
    authFuture.authComplete(new AuthResult(false, failReason));
  }

  public void authSuccess() {
    isAuthSuccess = true;
    authFuture.authComplete(AuthResult.AUTH_SUCCESS);
  }

  /**
   * 是否已经通过认证
   * @return  是否已经通过认证
   */
  public boolean isAuthSuccess() {
    return this.isAuthSuccess;
  }





}
