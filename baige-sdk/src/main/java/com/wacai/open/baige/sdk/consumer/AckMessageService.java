package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.sdk.ClientConfig;
import com.wacai.open.baige.sdk.MQClientInstance;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckMessageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AckMessageService.class);

  private final LinkedBlockingQueue<Runnable> ackRequestTaskQueue =
      new LinkedBlockingQueue<>(100000);

  private ThreadPoolExecutor ackExecutor;
  private ClientConfig clientConfig;


  private final MQClientInstance mqClientInstance;

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          return new Thread(r, "AckMessageServiceScheduleThread");
        }
      });


  public AckMessageService(MQClientInstance mqClientInstance) {
    this.mqClientInstance = mqClientInstance;
    this.clientConfig = mqClientInstance.getClientConfig();

  }

//  @Override
//  public String getServiceName() {
//    return AckMessageService.class.getSimpleName();
//  }
//
//  @Override
//  public void doLoopService() {
//
//    try {
//      AckRequest ackRequest = this.ackRequestQueue.take();
//      if (ackRequest != null) {
//        this.ackMsg(ackRequest);
//      }
//    } catch (InterruptedException e) {
//      LOGGER.error("take ackRequest from queue catch InterruptedException ", e);
//    } catch (Exception e) {
//      LOGGER.error("AckMessageService run method catch Exception ", e);
//    }
//
//  }



  public void executeAckRequestNow(AckRequest ackRequest) {
//    try {
//      this.ackRequestQueue.put(ackRequest);
//    } catch (InterruptedException e) {
//      LOGGER.warn("put ackRequest {} to queue catch exception", ackRequest, e);
//    }

    this.ackExecutor.submit(new AckRequestTask(ackRequest));

  }

  public void executeAckRequestLater(final AckRequest ackRequest, final long timeDelayMills) {
    this.scheduledExecutorService.schedule(new Runnable() {
      @Override
      public void run() {
        AckMessageService.this.executeAckRequestNow(ackRequest);
      }
    }, timeDelayMills, TimeUnit.MILLISECONDS);
  }

  public void start() {
    this.ackExecutor = new ThreadPoolExecutor(//
        clientConfig.getAckThreadNums(),
        clientConfig.getAckThreadNums(),
        1000 * 60,//
        TimeUnit.MILLISECONDS,//
        this.ackRequestTaskQueue,//
        new ThreadFactoryImpl("AckMessageServiceThread_"));
  }

  public void suspend() {

  }

  public void resume() {
  }

  public void shutdown() {
    if (this.ackExecutor != null) {
      this.ackExecutor.shutdown();
    }

  }


  private class AckRequestTask implements Runnable {

    private final AckRequest ackRequest;

    public AckRequestTask(final AckRequest ackRequest) {
      this.ackRequest = ackRequest;
    }

    @Override
    public void run() {
      this.ackMsg();
    }

    private void ackMsg() {
      final MQConsumerInner mqConsumerInner = mqClientInstance.selectConsumer(ackRequest.getConsumerGroup());
      if (mqConsumerInner != null) {
        mqConsumerInner.doAckMsg(ackRequest);
      } else {
        LOGGER.warn("No matched consumer for the AckRequest {}, drop it", ackRequest);
      }
//      LOGGER.warn("the ackRequestTaskQueue size is {}", ackRequestTaskQueue.size());
    }

  }


}
