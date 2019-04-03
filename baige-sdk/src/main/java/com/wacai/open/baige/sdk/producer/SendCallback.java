package com.wacai.open.baige.sdk.producer;

public interface SendCallback {

  void onSuccess(final SendResult sendResult);

  void onException(Throwable t);

}
