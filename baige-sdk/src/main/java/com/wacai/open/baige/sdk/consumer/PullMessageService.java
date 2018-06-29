package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.remoting.common.ServiceThread;
import com.wacai.open.baige.sdk.MQClientInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullMessageService  {


  private static final Logger LOGGER = LoggerFactory.getLogger(PullMessageService.class);
//  private final LinkedBlockingQueue<PullRequest> pullRequestQueue =
//      new LinkedBlockingQueue<>();

  private PullRequest pullRequest;

  private long  pullIntervalMs = 0;
  private int pullThreadsNum;


  private final MQClientInstance mqClientInstance;

  private List<PullMessageAtomService> pullMessageAtomServiceList;



  public PullMessageService(MQClientInstance mqClientInstance, PullRequest pullRequest, long pullIntervalMs, int pullThreadsNum) {
    this.mqClientInstance = mqClientInstance;
    this.pullRequest = pullRequest;
    this.pullIntervalMs = pullIntervalMs;
    this.pullThreadsNum = pullThreadsNum;

    this.pullMessageAtomServiceList = new ArrayList<>(pullThreadsNum);

    for (int i = 0; i < this.pullThreadsNum; ++i) {
      this.pullMessageAtomServiceList.add(new PullMessageAtomService(this.pullIntervalMs, i,1000));
    }
  }




  public void suspend() {

    for(PullMessageAtomService pullMessageAtomService : pullMessageAtomServiceList) {
      pullMessageAtomService.suspend();
    }

  }

  public void resume() {
    for(PullMessageAtomService pullMessageAtomService : pullMessageAtomServiceList) {
      pullMessageAtomService.resume();
    }
  }

  public void start() {
    for(PullMessageAtomService pullMessageAtomService : pullMessageAtomServiceList) {
      pullMessageAtomService.start();
    }
  }


  private class PullMessageAtomService extends ServiceThread {


    private final AtomicInteger pullMissCount = new AtomicInteger(0);
    private final int index;
    /*标记是否增加了拉取消息的时间间隔*/
    private AtomicBoolean delayPullInterval = new AtomicBoolean(false);
    private long pullIntervalMsWhenMsgNotFound = 1000;

    /*控制长轮询suspend的操作*/
    private AtomicBoolean isSuspendForLongPolling = new AtomicBoolean(false);


    public PullMessageAtomService(long pullIntervalMs, int index, long pullIntervalMsWhenMsgNotFound) {
      super(pullIntervalMs);
      this.index = index;
      this.pullIntervalMsWhenMsgNotFound = pullIntervalMsWhenMsgNotFound;
    }

    @Override
    public String getServiceName() {
      return PullMessageAtomService.class.getSimpleName();
    }

    @Override
    protected void doLoopService() {
      final MQConsumerInner mqConsumerInner = mqClientInstance.selectConsumer(pullRequest.getConsumerGroup());
      if (mqConsumerInner != null) {
        mqConsumerInner.doPullMessage(pullRequest, new PullMsgCallback() {
          @Override
          public void onSuccess(PullResult pullResult) {
            //拉取到消息以后，重新把拉取消息的时间间隔修正成原始值；
            delayPullInterval.set(false);
            LOGGER.info("PullMessageAtomService_{} set pull msg interval to {} ms, Reason:Msg found",
                index, pullIntervalMs);
            setLoopInterval(pullIntervalMs);
          }

          @Override
          public void onException(Throwable e) {

          }

          @Override
          public void onMsgNotFound(PullRequest pullRequest) {
//            if (isSuspendForLongPolling.compareAndSet(false, true)) {
//              try {
//                long suspendMs = mqClientInstance.getClientConfig().getSuspendTimeoutMsForLongPolling();
//                LOGGER.info("suspend {} ms for long polling {},  reason is msg not found",
//                    suspendMs,
//                    pullRequest);
//                suspend(suspendMs);
//                LOGGER.info("exit suspend for long polling {} after {} ms", pullRequest, suspendMs);
//              } finally {
//                isSuspendForLongPolling.set(false);
//              }
//            }

          }
        });
      } else {
        LOGGER.warn("No matched consumer for the PullRequest {}, drop it", pullRequest);
      }

    }
  }
}
