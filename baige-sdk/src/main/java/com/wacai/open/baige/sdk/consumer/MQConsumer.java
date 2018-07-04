package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.sdk.MQAdmin;
import com.wacai.open.baige.sdk.consumer.listener.MessageListener;

public interface MQConsumer extends MQAdmin {


  void start();

  void shutdown();

  void suspend();




  /**
   *  多次注册相同topic的{@link MessageListener} , 以第一个为准。
   * @param topic  消息主题
   * @param messageListener 消息监听器
   * @return MQConsumer MQConsumer
   */
  MQConsumer registerMessageListener(String topic, final MessageListener messageListener);


  /**
  void suspend();

  void resume();
  */


}
