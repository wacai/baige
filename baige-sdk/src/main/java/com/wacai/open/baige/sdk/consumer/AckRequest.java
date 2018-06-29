package com.wacai.open.baige.sdk.consumer;

public class AckRequest {

  private String topic;
  private String consumerId;
  private String consumerGroup;
  private long offset;

  public AckRequest(String topic, String consumerId, String consumerGroup, long offset) {
    this.topic = topic;
    this.consumerId = consumerId;
    this.consumerGroup = consumerGroup;
    this.offset = offset;
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

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }


  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("AckRequest[");
    s.append("topic=").append(this.topic).append(",");
    s.append("consumerId=").append(this.consumerId).append(",");
    s.append("consumerGroup=").append(this.consumerGroup).append(",");
    s.append("offset=").append(this.offset).append(",");
    s.append("]");
    return s.toString();
  }
}
