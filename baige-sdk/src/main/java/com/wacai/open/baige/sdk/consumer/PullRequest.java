package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.sdk.consumer.listener.MessageListener;

public class PullRequest {

  private String consumerGroup;
  private String topic;
  private String consumerId;


  private MessageListener messageListener;





  public PullRequest(String consumerGroup, String topic) {
    this(consumerGroup, null, topic); 
  }

  public PullRequest(String consumerGroup, String consumerId, String topic) {
    this(consumerGroup, consumerId, topic, null);
  }




  public PullRequest(String consumerGroup, String consumerId, String topic,
      MessageListener messageListener) {

    this.consumerGroup = consumerGroup;
    this.consumerId = consumerId;
    this.topic = topic;
    this.messageListener = messageListener;
  }


  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(String consumerId) {
    this.consumerId = consumerId;
  }



  public MessageListener getMessageListener() {
    return messageListener;
  }

  public void setMessageListener(
      MessageListener messageListener) {
    this.messageListener = messageListener;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("PullRequest[");
    s.append("topic=").append(this.topic).append(",");
    s.append("consumerGroup=").append(this.consumerGroup).append(",");
    s.append("consumerId=").append(this.consumerId).append(",");
    s.append("]");
    return s.toString();

  }
}
