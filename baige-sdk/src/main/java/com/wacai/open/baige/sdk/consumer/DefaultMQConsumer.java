package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.common.protocol.consume.ConsumeUtil;
import com.wacai.open.baige.common.util.MiscUtil;
import com.wacai.open.baige.remoting.common.RemotingUtil;
import com.wacai.open.baige.sdk.ClientConfig;
import com.wacai.open.baige.sdk.consumer.listener.ConsumeStatus;
import com.wacai.open.baige.sdk.consumer.listener.MessageListener;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMQConsumer extends ClientConfig implements MQConsumer {

  private static Logger LOGGER = LoggerFactory.getLogger(DefaultMQConsumer.class);

  private static final AtomicInteger serialNo = new AtomicInteger(0XAAAA);


  protected final transient DefaultMQConsumerInnerImpl defaultMQConsumerInnerImpl;

  private String consumerGroup;




  public DefaultMQConsumer(String appKey, String appSecret, String wsServerURL) {
    this.consumerGroup = ConsumeUtil.getConsumerGroupName(appKey);
    this.setAppKey(appKey);
    this.setAppSecret(appSecret);
    this.setWsServerURL(wsServerURL);
    this.defaultMQConsumerInnerImpl = new DefaultMQConsumerInnerImpl(this);

  }

  @Override
  public void start()  {
    try {
      this.defaultMQConsumerInnerImpl.start();
    } catch (Exception e) {
      LOGGER.error("start DefaultMQConsumerInnerImpl catch Exception" , e);
      this.shutdown();
    }

  }

  @Override
  public void shutdown() {
    this.defaultMQConsumerInnerImpl.shutdown();
  }

  @Override
  public void suspend() {
    this.defaultMQConsumerInnerImpl.suspend();
  }

  @Override
  public MQConsumer registerMessageListener(String topic, MessageListener messageListener) {
    this.defaultMQConsumerInnerImpl.registerMessageListener(topic, messageListener);
    return this;
  }






  public  String getConsumerID() {
    StringBuilder consumerID = new StringBuilder();
    String ipV4 = RemotingUtil.getLocalAddress();
    consumerID.append(ipV4);

    consumerID.append("-");
    Long pid = MiscUtil.getPID();
    consumerID.append(pid);

//    consumerID.append("-");
//    consumerID.append(String.format("%04X", serialNo.getAndIncrement()));

    return consumerID.toString();

  }

  /*getter and setter methods*/

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }




}
