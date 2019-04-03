package com.wacai.open.baige.sdk.example;

import com.wacai.open.baige.remoting.exception.RemotingException;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;
import com.wacai.open.baige.sdk.producer.DefaultMQProducer;
import com.wacai.open.baige.sdk.producer.SendCallback;
import com.wacai.open.baige.sdk.producer.SendResult;
import com.wacai.open.baige.sdk.producer.SendStatus;

import java.io.IOException;

public class DefaultMQProducerDemo {

    public static void main(String[] args)
        throws IOException, RemotingException, ClientException, InterruptedException, AuthException {

    /*
      测试环境：
      */
        String appKey = "mmtnqybb4qvk";
        String appSecret = "39e323c8a3be40a081f29a7ef8d8034b";
        String wsServerURL = "ws://open.mq.test.wacai.info/ws";

        //消息投递。
        DefaultMQProducer defaultMQProducer = new DefaultMQProducer(appKey, appSecret, wsServerURL);
        defaultMQProducer.start();

        String msg = "{\"category\":\"appInfo\",\"eventType\":\"U\",\"properties\":{\"appId\":301,\"appKey\":\"7an6femkhkn7\",\"authenticationPath\":\"\"}}";

        try {
            defaultMQProducer.send("bullseye.feedback.third.approve", msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    if (SendStatus.OK == sendResult.getSendStatus()) {
                        System.out.println("OK");
                    } else {
                        //消息发送失败
                    }
                }

                @Override
                public void onException(Throwable t) {
                    //消息发送失败，通过t.getMessage() 打印具体异常信息（业务可以不关心）
                    System.out.println(t.getMessage());
                }
            }, 3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
