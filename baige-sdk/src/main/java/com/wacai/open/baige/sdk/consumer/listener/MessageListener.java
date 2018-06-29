package com.wacai.open.baige.sdk.consumer.listener;

import com.wacai.open.baige.common.message.Message;

public interface MessageListener {

  ConsumeStatus consumeMessages(Message message);

}
