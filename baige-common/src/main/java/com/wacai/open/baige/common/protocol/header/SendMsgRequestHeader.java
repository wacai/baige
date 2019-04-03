package com.wacai.open.baige.common.protocol.header;

public class SendMsgRequestHeader implements CommandCustomHeader {

  private String producerGroup;
  private String topic;

  @Override
  public void checkFields() {

  }


  public String getProducerGroup() {
    return producerGroup;
  }

  public void setProducerGroup(String producerGroup) {
    this.producerGroup = producerGroup;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }
}
