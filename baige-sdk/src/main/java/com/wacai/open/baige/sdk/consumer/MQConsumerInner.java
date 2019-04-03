package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.common.protocol.heartbeat.SubscriptionData;
import java.util.List;
import java.util.Set;

public interface MQConsumerInner {

  String groupName();

  Set<SubscriptionData> subscriptions();

  void doPullMessage(PullRequest pullRequest, PullMsgCallback pullMsgCallback);

//  void doPullMessage(PullRequest pullRequest);

  void doAckMsg(AckRequest ackRequest);


  void executeAckRequestNow(final AckRequest ackRequest);


  /**
   * 唤醒sdk恢复消息拉取。
   * @param consumerGroup 消费者分组
   * @param topic 消息主题
   * @param messageList 消息代理代为拉取到的消息列表
   * @return  唤醒sdk恢复消息拉取是否成功
   */
  boolean resumePullMsg(String consumerGroup, String topic, List<Message> messageList);


  /**
   *  拉取消息的请求 获得了若干条消息；
   * @param pullRequest 拉取消息请求
   * @param messages 拉取到的消息
   */
  void recvMessageList(PullRequest pullRequest, List<Message> messages);



}
