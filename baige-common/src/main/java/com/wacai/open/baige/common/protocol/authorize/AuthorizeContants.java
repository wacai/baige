package com.wacai.open.baige.common.protocol.authorize;

/**
 *  和授权认证相关的常量
 */
public interface AuthorizeContants {

  /**
   * 授权认证相关的http 常量
   */
  interface HttpConstants {

    /**
     * 授权认证相关的HTTP header 。
     */
    interface  Headers {


      String  AUTH_FAIL_REASON = "auth-fail-reason";

      String AUTH_INFO = "x-wac-mq-auth-info";

      String SERVER_ID = "server-id";

    }


  }

  interface AuthFailReason {
      String NO_AUTH_INFO = "no authorize info";
      String AUTH_INFO_ILLEGAL_FORMAT = "authorize info is illegal, the format is like this: {'appkey':'12345','plainText':'12345','sign':'12345'}";
      String SIGN_IS_NOT_CORRECT = "sign is not correct";
      String NOT_PATH_AUTHORIZED = "not pass authorize";
      String NO_SERVER_ID = "no server-id http header";

  }

}
