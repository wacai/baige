package com.wacai.open.baige.sdk.consumer;

public interface AckMsgCallback {

  void onSuccess(AckRequest ackRequest);

  void onException(AckRequest ackRequest, Throwable t);

}
