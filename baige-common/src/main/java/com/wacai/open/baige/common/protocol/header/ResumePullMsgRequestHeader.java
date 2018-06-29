package com.wacai.open.baige.common.protocol.header;

public class ResumePullMsgRequestHeader  implements CommandCustomHeader{

  private String consumerGroup;
  private String topic;

  @Override
  public void checkFields() {

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
}
