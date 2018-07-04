package com.wacai.open.baige.sdk;

import com.wacai.open.baige.remoting.common.RemotingUtil;

public class ClientConfig {


  /**
   * 多长时间发送心跳包给broker .
   * Heartbeat interval in microseconds with message broker
   */
  private int heartbeatBrokerInterval = 1000 * 3;

  /*发送心跳的超时时间*/
  private int heartbeartSendTimeoutMs = 3000;

  private int connectTimeousMs = 5000;

  /*发送认证请求的超时时间*/
  private long authorizeTimeoutMs = 10000;

  /*发送ack请求的超时时间*/
  private long ackTimeoutMs = 3000;

  /*两次拉取消息的时间间隔， 单位：ms*/
  private long pullTimeIntervalMs = 0;

  /*同一时间做消息拉取的线程数*/
  private int pullThreadsNum = 3;

  /*消费线程的数量*/
  private int consumeThreadNums = 20;

  /*ack线程的数量*/
  private int ackThreadNums = 5;

  /*单条消息的最大消费次数*/
  private int maxConsumeTimes = 3;

  /*拉取不到消息时， 长轮询的最大等待时间， 单位：ms */
  private long suspendTimeoutMsForLongPolling = 30000;

  /*一次拉取消息的timeout时间，单位：Ms*/
  private long pullTimeoutMs = 5000;

  private String clientIP = RemotingUtil.getLocalAddress();

  private String instanceName = System.getProperty("baige.sdk.client.name", "DEFAULT");

  private int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();



  /**
   * bridge server的URL：，
   * 格式：ws://ip:port/path
   */
  private String wsServerURL;

  private String appKey;

  private String appSecret;


  public String buildMQClientId() {
    StringBuilder clientId = new StringBuilder();
    clientId.append(this.getClientIP());
    clientId.append("@");
    clientId.append(this.getInstanceName());
    return clientId.toString();
  }

  public int getHeartbeartSendTimeoutMs() {
    return heartbeartSendTimeoutMs;
  }

  public void setHeartbeartSendTimeoutMs(int heartbeartSendTimeoutMs) {
    this.heartbeartSendTimeoutMs = heartbeartSendTimeoutMs;
  }

  public int getConnectTimeousMs() {
    return connectTimeousMs;
  }

  public void setConnectTimeousMs(int connectTimeousMs) {
    this.connectTimeousMs = connectTimeousMs;
  }

  public long getAuthorizeTimeoutMs() {
    return authorizeTimeoutMs;
  }




  public long getAckTimeoutMs() {
    return ackTimeoutMs;
  }

  public void setAckTimeoutMs(long ackTimeoutMs) {
    this.ackTimeoutMs = ackTimeoutMs;
  }


  public long getPullTimeIntervalMs() {
    return pullTimeIntervalMs;
  }

  public void setPullTimeIntervalMs(long pullTimeIntervalMs) {
    this.pullTimeIntervalMs = pullTimeIntervalMs;
  }


  public int getPullThreadsNum() {
    return pullThreadsNum;
  }

  public void setPullThreadsNum(int pullThreadsNum) {
    this.pullThreadsNum = pullThreadsNum;
  }

  public void setAuthorizeTimeoutMs(long authorizeTimeoutMs) {
    this.authorizeTimeoutMs = authorizeTimeoutMs;
  }

  public int getConsumeThreadNums() {
    return consumeThreadNums;
  }

  public void setConsumeThreadNums(int consumeThreadNums) {
    this.consumeThreadNums = consumeThreadNums;
  }


  public int getAckThreadNums() {
    return ackThreadNums;
  }

  public void setAckThreadNums(int ackThreadNums) {
    this.ackThreadNums = ackThreadNums;
  }

  public int getMaxConsumeTimes() {
    return maxConsumeTimes;
  }

  public void setMaxConsumeTimes(int maxConsumeTimes) {
    this.maxConsumeTimes = maxConsumeTimes;
  }

  public long getSuspendTimeoutMsForLongPolling() {
    return suspendTimeoutMsForLongPolling;
  }

  public void setSuspendTimeoutMsForLongPolling(long suspendTimeoutMsForLongPolling) {
    this.suspendTimeoutMsForLongPolling = suspendTimeoutMsForLongPolling;
  }

  public long getPullTimeoutMs() {
    return pullTimeoutMs;
  }

  public void setPullTimeoutMs(long pullTimeoutMs) {
    this.pullTimeoutMs = pullTimeoutMs;
  }

  public int getHeartbeatBrokerInterval() {
    return heartbeatBrokerInterval;
  }

  public void setHeartbeatBrokerInterval(int heartbeatBrokerInterval) {
    this.heartbeatBrokerInterval = heartbeatBrokerInterval;
  }

  public String getClientIP() {
    return clientIP;
  }

  public void setClientIP(String clientIP) {
    this.clientIP = clientIP;
  }

  public String getInstanceName() {
    return instanceName;
  }

  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }


  public int getClientCallbackExecutorThreads() {
    return clientCallbackExecutorThreads;
  }


  public void setClientCallbackExecutorThreads(int clientCallbackExecutorThreads) {
    this.clientCallbackExecutorThreads = clientCallbackExecutorThreads;
  }


  public String getWsServerURL() {
    return wsServerURL;
  }

  public void setWsServerURL(String wsServerURL) {
    this.wsServerURL = wsServerURL;
  }



  public String getAppKey() {
    return appKey;
  }

  public void setAppKey(String appKey) {
    this.appKey = appKey;
  }

  public String getAppSecret() {
    return appSecret;
  }

  public void setAppSecret(String appSecret) {
    this.appSecret = appSecret;
  }


  @Override
  public String toString() {
    StringBuilder clientConfig = new StringBuilder();
    clientConfig.append("ClientConfig [");
    clientConfig.append("clientIP=").append(clientIP).append(",");
    clientConfig.append("instanceName=").append(instanceName).append(",");
    clientConfig.append("clientCallbackExecutorThreads=").append(clientCallbackExecutorThreads).append(",");
    clientConfig.append("wsServerURL=").append(wsServerURL).append(",");
    clientConfig.append("appKey=").append(appKey).append(",");
    clientConfig.append("appSecret=").append(appSecret).append("");
    clientConfig.append("]");
    return clientConfig.toString();
  }

}
