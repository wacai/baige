package com.wacai.open.baige.sdk.example.quickstart;

import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.sdk.consumer.DefaultMQConsumer;
import com.wacai.open.baige.sdk.consumer.listener.ConsumeStatus;
import com.wacai.open.baige.sdk.consumer.listener.MessageListener;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultMQConsumerDemo {

  public static void main(String []args) throws ClientException, InterruptedException, AuthException {

    /*
      测试环境：
      */
    String appKey = "mvkpfcb9uec7";
    String appSecret = "71ba8b293c2c4e0ebdbf8ac361ee3d92";
    String wsServerURL = "ws://open.wacaiyun.com/ws";


    //创建消息消费者
    DefaultMQConsumer defaultMQConsumer = new DefaultMQConsumer(
        appKey, appSecret, wsServerURL);


     /*控制两次拉取消息间的时间间隔,单位：ms；如果不设置，则不停拉取； */
    defaultMQConsumer.setPullTimeIntervalMs(500);

    /*控制拉取消息的线程数*/
    defaultMQConsumer.setPullThreadsNum(1);

     /*控制消费线程数量，不设置此参数，则默认消费线程数是20*/
    defaultMQConsumer.setConsumeThreadNums(1);

    /*控制ack消息的线程数量*/
    defaultMQConsumer.setAckThreadNums(1);

     /*单条消息的最大消费次数, 默认是3*/
    defaultMQConsumer.setMaxConsumeTimes(3);

    defaultMQConsumer.setConnectTimeousMs(5000);

    defaultMQConsumer.setAuthorizeTimeoutMs(20000);
    /**
     * 注册消息处理函数， 如果订阅了多个topic，则调用多次；
     * 相同的topic重复调用，则以第一次为准；
     */

    final AtomicInteger recvMsgCount = new AtomicInteger(0);
    final int metricIntervalSeconds = 5;


    ScheduledExecutorService scheduledExecutorService =   Executors
        .newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
            "metricThread"));

    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        int count = recvMsgCount.getAndSet(0);
        System.out.println("recv msg tps:" + count / metricIntervalSeconds);
      }
    }, 0, metricIntervalSeconds, TimeUnit.SECONDS);


    //middleware.guard.cache
    //loan.open.scene.approve.result.xhj
    defaultMQConsumer.registerMessageListener("middleware.guard.cache", new MessageListener() {
      @Override
      public ConsumeStatus consumeMessages(Message message) {
           /*消息处理逻辑，不要在这里进行阻塞操作； */
        System.out.println("recv msg:{}" + new String(message.getPayload()));
        recvMsgCount.addAndGet(1);
        return ConsumeStatus.SUCCESS;
      }
    });

      /*启动消息消费*/
    defaultMQConsumer.start();
  }

}
