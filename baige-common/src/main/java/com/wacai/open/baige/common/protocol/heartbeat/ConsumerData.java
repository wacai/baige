package com.wacai.open.baige.common.protocol.heartbeat;

import java.util.Set;

public class ConsumerData {

  private String groupName;

  private Set<SubscriptionData> subscriptionDataSet;


  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public Set<SubscriptionData> getSubscriptionDataSet() {
    return subscriptionDataSet;
  }

  public void setSubscriptionDataSet(
      Set<SubscriptionData> subscriptionDataSet) {
    this.subscriptionDataSet = subscriptionDataSet;
  }

  @Override
  public String toString() {
    return "ConsumerData [groupName=" + groupName
        + ", subscriptionDataSet=" + subscriptionDataSet + "]";
  }
}
