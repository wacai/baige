package com.wacai.open.baige.sdk.producer;

import com.wacai.open.baige.common.protocol.produce.ProduceUtil;
import com.wacai.open.baige.remoting.exception.RemotingException;
import com.wacai.open.baige.sdk.ClientConfig;
import com.wacai.open.baige.sdk.ClientState;
import com.wacai.open.baige.sdk.consumer.DefaultMQConsumer;
import com.wacai.open.baige.sdk.consumer.DefaultMQConsumerInnerImpl;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMQProducer extends ClientConfig implements  MQProducer {

  private static Logger LOGGER = LoggerFactory.getLogger(DefaultMQProducer.class);

  private String producerGroup;


  protected final transient DefaultMQProducerInnerImpl defaultMQProducerInnerImpl;

  public DefaultMQProducer(String appKey, String appSecret, String wsServerURL) {
    this.producerGroup = ProduceUtil.getProducerGroup(appKey);
    this.setAppKey(appKey);
    this.setAppSecret(appSecret);
    this.setWsServerURL(wsServerURL);
    this.defaultMQProducerInnerImpl = new DefaultMQProducerInnerImpl(this);
  }

  @Override
  public void start() throws AuthException, ClientException {
    try {
      this.defaultMQProducerInnerImpl.start();
    } catch (AuthException | ClientException e) {
      this.defaultMQProducerInnerImpl.shutdown();
      throw e;
    }
  }

  @Override
  public void shutdown() {
    this.defaultMQProducerInnerImpl.shutdown();
  }

//  @Override
//  public void send(String topic, byte[] msgBody, SendCallback sendCallback,
//      long timeoutMills) throws RemotingException, ClientException, InterruptedException {
//    this.defaultMQProducerInnerImpl.send(topic, msgBody, sendCallback, timeoutMills);
//  }

  @Override
  public void send(String topic, String msgBody, SendCallback sendCallback, long timeoutMills)
      throws ClientException, RemotingException, InterruptedException {
    this.defaultMQProducerInnerImpl.send(topic, msgBody, sendCallback, timeoutMills);
  }


  public String getProducerGroup() {
    return producerGroup;
  }
}
