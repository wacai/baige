package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.sdk.consumer.listener.ConsumeStatus;
import com.wacai.open.baige.sdk.consumer.listener.MessageListener;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ConsumeMessageService {

  private static Logger LOGGER = LoggerFactory.getLogger(ConsumeMessageService.class);


  private final ThreadPoolExecutor consumeExecutor;
  private final BlockingQueue<Runnable> consumeRequestQueue;

  private final DefaultMQConsumer defaultMQConsumer;
  private final DefaultMQConsumerInnerImpl defaultMQConsumerInnerImpl;


  public ConsumeMessageService(DefaultMQConsumer defaultMQConsumer,
      DefaultMQConsumerInnerImpl defaultMQConsumerInnerImpl) {
    this.consumeRequestQueue = new LinkedBlockingQueue<Runnable>();
    this.defaultMQConsumer = defaultMQConsumer;
    this.defaultMQConsumerInnerImpl = defaultMQConsumerInnerImpl;
    this.consumeExecutor = new ThreadPoolExecutor(//
        this.defaultMQConsumer.getConsumeThreadNums(),//
        this.defaultMQConsumer.getConsumeThreadNums(),//
        1000 * 60,//
        TimeUnit.MILLISECONDS,//
        this.consumeRequestQueue,//
        new ThreadFactoryImpl("ConsumeMessageServiceThread_"));


  }
  public void submitConsumeRequest(PullRequest pullRequest,
      PullResult pullResult, MessageListener messageListener) {

    ConsumeRequest consumeRequest = new ConsumeRequest(pullRequest, pullResult, messageListener);
    this.consumeExecutor.submit(consumeRequest);

  }

  public void shutdown() {
    if (this.consumeExecutor != null) {
      this.consumeExecutor.shutdown();
    }
  }

  public void start() {}


  private class ConsumeRequest implements  Runnable {

    private final MessageListener messageListener;
    private final PullRequest pullRequest;
    private final PullResult pullResult;

    public ConsumeRequest(PullRequest pullRequest, PullResult pullResult,  MessageListener messageListener) {
      this.pullRequest = pullRequest;
      this.messageListener = messageListener;
      this.pullResult = pullResult;
    }

    @Override
    public void run() {
      ConsumeStatus consumeStatus = null;
      List<Message> messageList = pullResult.getMsgList();

      if (messageList == null || messageList.size() <= 0) {
          LOGGER.warn("pull consume messageList is null");
        return;
      }

      for (Message message : messageList) {
        if (message == null) {
            LOGGER.warn("pull consume msg is null");
            continue;
        }
        for (int i = 0; i < defaultMQConsumer.getMaxConsumeTimes(); ++i) {
          try {
            consumeStatus = messageListener.consumeMessages(message);
          } catch (Exception e) {
              LOGGER.error("deal msg:{},exception:{}",message,e.getMessage());
            consumeStatus = ConsumeStatus.FAILURE;
          }
          if (consumeStatus == ConsumeStatus.SUCCESS) {
            break;
          }
        }
        if (ConsumeStatus.SUCCESS == consumeStatus) {
          LOGGER.info("consume msg {} success", message);
        } else {
          LOGGER.warn("consume msg {} failure", message);
        }
      /*无论是否消费成功，都要发起ack请求*/
        AckRequest ackRequest = new AckRequest(
            pullRequest.getTopic(),
            pullRequest.getConsumerId(),
            pullRequest.getConsumerGroup(),
            message.getOffset()
        );
       defaultMQConsumerInnerImpl.executeAckRequestNow(ackRequest);
      }
    }
  }
}
