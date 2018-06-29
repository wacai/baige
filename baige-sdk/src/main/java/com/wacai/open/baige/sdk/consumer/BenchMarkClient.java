package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.common.ThreadFactoryImpl;
import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.sdk.consumer.listener.ConsumeStatus;
import com.wacai.open.baige.sdk.consumer.listener.MessageListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchMarkClient {

  public static void main(String []args) {
//    String appKey = System.getProperty("appKey", "xbh8t9d79cq4");
//    String appSecret = System.getProperty("appSecret", "1002a8edebc84fbab95a7cd4eed7376d");
//    String wsServerURL = System.getProperty("wsServerURL", "ws://172.16.47.33:8888/ws");

    //appKey和appSecret请参考前面的文档获取；
    String appKey = "cw889h5uyqhd";
    String appSecret = "6876047fa0934224949674fdcfe360a3";


    //测试环境服务器地址
    String wsServerURL = "ws://baige-bridge-8888.loan.k2.test.wacai.info/ws";

//    String wsServerURL = "ws://172.16.69.165:8888/ws";

    long pullTimeIntervalMs = Long.parseLong(System.getProperty("pullTimeIntervalMs", "500"));
    int pullThreadsNum = Integer.parseInt(System.getProperty("pullThreadsNum", "1"));
    int consumeThreadsNum = Integer.parseInt(System.getProperty("consumeThreadsNum", "2"));
    int ackThreadsNum = Integer.parseInt(System.getProperty("ackThreadsNum", "2"));
    int maxConsumeTimes = Integer.parseInt(System.getProperty("maxConsumeTimes", "2"));
    int connectTimeousMs = Integer.parseInt(System.getProperty("connectTimeousMs", "5000"));
    int authorizeTimeoutMs = Integer.parseInt(System.getProperty("authorizeTimeoutMs", "5000"));
    int metricIntervalSeconds = Integer.parseInt(System.getProperty("metricIntervalSeconds", "5"));
    int pullTimeoutMs = Integer.parseInt(System.getProperty("pullTimeoutMs", "5000"));
    int ackTimeoutMs = Integer.parseInt(System.getProperty("ackTimeoutMs", "5000"));








    //创建消息消费者
    DefaultMQConsumer defaultMQConsumer = new DefaultMQConsumer(
        appKey, appSecret, wsServerURL);


     /*控制两次拉取消息间的时间间隔,单位：ms；如果不设置，则不停拉取； */
    defaultMQConsumer.setPullTimeIntervalMs(pullTimeIntervalMs);

    /*控制拉取消息的线程数*/
    defaultMQConsumer.setPullThreadsNum(pullThreadsNum);

     /*控制消费线程数量，不设置此参数，则默认消费线程数是20*/
    defaultMQConsumer.setConsumeThreadNums(consumeThreadsNum);

    /*控制ack消息的线程数量*/
    defaultMQConsumer.setAckThreadNums(ackThreadsNum);

     /*单条消息的最大消费次数, 默认是3*/
    defaultMQConsumer.setMaxConsumeTimes(maxConsumeTimes);

    defaultMQConsumer.setConnectTimeousMs(connectTimeousMs);

    defaultMQConsumer.setAuthorizeTimeoutMs(authorizeTimeoutMs);

    defaultMQConsumer.setPullTimeoutMs(pullTimeoutMs);

    defaultMQConsumer.setAckTimeoutMs(ackTimeoutMs);

    /**
     * 注册消息处理函数， 如果订阅了多个topic，则调用多次；
     * 相同的topic重复调用，则以第一次为准；
     */

    final AtomicInteger recvMsgCount = new AtomicInteger(0);


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


    defaultMQConsumer.registerMessageListener("middleware.guard.cache", new MessageListener() {
      @Override
      public ConsumeStatus consumeMessages(Message message) {
        System.out.println("recv msg:" + message);
           /*消息处理逻辑，不要在这里进行阻塞操作； */
        recvMsgCount.addAndGet(1);
        return ConsumeStatus.SUCCESS;
      }
    });

      /*启动消息消费*/
    defaultMQConsumer.start();

  }

}
