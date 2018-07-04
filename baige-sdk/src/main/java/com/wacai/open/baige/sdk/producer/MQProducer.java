package com.wacai.open.baige.sdk.producer;

import com.wacai.open.baige.remoting.exception.RemotingException;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;

public interface MQProducer {

  void start() throws AuthException, ClientException;
  void shutdown();

//  void send(String topic, byte[] msgBody, final SendCallback sendCallback, long timeoutMills)
//      throws ClientException, RemotingException, InterruptedException;

  void send(String topic, String msgBody, final SendCallback sendCallback, long timeoutMills)
      throws ClientException, RemotingException, InterruptedException;





}
