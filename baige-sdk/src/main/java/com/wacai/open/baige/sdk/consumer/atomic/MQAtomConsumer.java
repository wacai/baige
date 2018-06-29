package com.wacai.open.baige.sdk.consumer.atomic;

import com.wacai.open.baige.common.LifecycleItem;
import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.sdk.MQClientInstance;
import com.wacai.open.baige.sdk.consumer.MQConsumerInner;
import com.wacai.open.baige.sdk.consumer.PullMessageService;
import com.wacai.open.baige.sdk.consumer.PullRequest;
import com.wacai.open.baige.sdk.consumer.listener.MessageListener;
import java.util.List;

/**
 * 负责一个消费者分组下的一个消费者消费指定的Topic。
 *
 *
 */
public class MQAtomConsumer extends LifecycleItem {


  private String consumerGroup;
  private final String consumerId;
  private String topic;

  private PullRequest pullRequest;

  private MessageListener messageListener;

  private MQConsumerInner mqConsumerInner;

  private PullMessageService pullMessageService;


  private long  pullIntervalMs = 0;

  private int pullThreadsNum;



//  private Looper looper;

  public MQAtomConsumer(String consumerGroup, String consumerId, String topic,  MessageListener messageListener,
      MQClientInstance mqClientInstance, long pullIntervalMs, int pullThreadsNum) {
    super("MQAtomConsumer");
    this.consumerGroup = consumerGroup;
    this.consumerId = consumerId;
    this.topic = topic;
    this.messageListener = messageListener;
    this.pullIntervalMs = pullIntervalMs;
    this.pullThreadsNum = pullThreadsNum;


    this.pullRequest = new PullRequest(
        this.consumerGroup,
        this.consumerId,
        this.topic,
        this.messageListener);

    this.pullMessageService = new PullMessageService(mqClientInstance, this.pullRequest, this.pullIntervalMs, this.pullThreadsNum);

  }

  public MQAtomConsumer setMessageListener(MessageListener messageListener) {
    this.messageListener = messageListener;
    return this;
  }

  public MQAtomConsumer setMQConsumerInner(MQConsumerInner mqConsumerInner) {
    this.mqConsumerInner = mqConsumerInner;
    return this;
  }



  public MQAtomConsumer setPullIntervalMs(long pullIntervalMs) {
    if (pullIntervalMs > 0) {
      this.pullIntervalMs = pullIntervalMs;
    }
    return this;
  }


  public void recvMsgList(List<Message> messageList) {

    this.mqConsumerInner.recvMessageList(this.pullRequest, messageList);

  }

  public boolean resumePullMsg(List<Message> messages) {

    if (messages != null && messages.size() > 0) {
      try {
        LOGGER.info(
            "MQAtomConsumer recv {} messages when resume pull msg  [consumerGroup:{}, consumerId:{}, topic:{}] "
            , messages.size(), consumerGroup, consumerId, topic);
        this.recvMsgList(messages);
      } catch (Exception e) {
        LOGGER.warn("MQAtomConsumer recvMsgList catch Exception", e);
      }
    }

    try {
      this.resume();
      LOGGER.info("MQAtomConsumer resume pull msg [consumerGroup:{}, consumerId:{}, topic:{}] successfully ",
          consumerGroup, consumerId, topic);
    } catch (Exception e) {
      LOGGER.warn("MQAtomConsumer resume pull msg [consumerGroup:{}, consumerId:{}, topic:{}] catch Exception ",
          consumerGroup, consumerId, topic, e);
      return false;
    }

    return true;


  }


  @Override
  protected void doStart() throws Exception {
    assert(this.messageListener != null && this.mqConsumerInner != null);
//    this.mqConsumerInner.executePullRequestNow(this.pullRequest);
    this.pullMessageService.start();
  }

  @Override
  protected void doStop() {

  }


  public void suspend() {
    if (this.pullMessageService != null) {
      this.pullMessageService.suspend();
    }
  }


  public void resume() {
    if (this.pullMessageService != null) {
      this.pullMessageService.resume();
    }
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int hashcode = 1;
    if (this.consumerGroup != null) {
      hashcode = hashcode * prime + this.consumerGroup.hashCode();
    }
    if (this.consumerId != null) {
      hashcode = hashcode * prime + this.consumerId.hashCode();
    }
    if (this.topic != null) {
      hashcode = hashcode * prime + this.topic.hashCode();
    }
    return hashcode;

  }


  @Override
  public boolean equals(Object object) {
    if (object == null || !(object instanceof MQAtomConsumer)) {
      return false;
    }
    MQAtomConsumer mqAtomConsumer = (MQAtomConsumer)object;
    if (mqAtomConsumer.consumerGroup == null || !mqAtomConsumer.consumerGroup.equals(this.consumerGroup)) {
      return false;
    }
    if (mqAtomConsumer.consumerId == null || !mqAtomConsumer.consumerId.equals(this.consumerId)) {
      return false;
    }
    if (mqAtomConsumer.topic == null || !mqAtomConsumer.topic.equals(this.topic)) {
      return false;
    }
    return true;
  }


  /*getter and setter*/

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public String getTopic() {
    return topic;
  }
}
