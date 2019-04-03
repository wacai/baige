package com.wacai.open.baige.common.protocol.heartbeat;


public class SubscriptionData implements Comparable<SubscriptionData> {

  private String topic;

  public SubscriptionData(String topic) {
    this.topic = topic;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  @Override
  public int compareTo(SubscriptionData o) {
    return this.topic.compareTo(o.getTopic());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.topic == null) ? 0 : this.topic.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || !(object instanceof SubscriptionData)) {
      return false;
    }
    SubscriptionData  subscriptionData = (SubscriptionData)object;
    return subscriptionData.topic != null && subscriptionData.topic.equals(this.topic);
  }

  @Override
  public String toString() {
    return "SubscriptionData [topic=" + this.topic  + "]";
  }


}
