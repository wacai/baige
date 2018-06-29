package com.wacai.open.baige.common.protocol.heartbeat;

import com.wacai.open.baige.common.protocol.RemotingSerializable;

public class HeartbeatData extends RemotingSerializable {

  private String clientId;
  private ConsumerData consumerData;

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public ConsumerData getConsumerData() {
    return consumerData;
  }

  public void setConsumerData(ConsumerData consumerData) {
    this.consumerData = consumerData;
  }

  @Override
  public String toString() {
    return "HeartbeatData [clientID=" + this.clientId + ", consumerData=" + consumerData + "]";
  }
}
