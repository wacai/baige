package com.wacai.open.baige.sdk.producer;

import com.wacai.open.baige.remoting.exception.RemotingException;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;

public interface MQProducerInner {

  void start() throws ClientException, AuthException;

  void shutdown();

  void send(String topic, String msgBody, SendCallback sendCallback, long timeoutMills)
      throws ClientException, RemotingException, InterruptedException;
}
