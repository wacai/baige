package com.wacai.open.baige.sdk.consumer;

import com.wacai.open.baige.common.message.Message;
import java.util.Collections;
import java.util.List;

public class PullResult {

  private final PullStatus pullStatus;
  private final List<Message> msgList;


  public PullResult(PullStatus pullStatus) {
    this(pullStatus, null);
  }


  public PullResult(PullStatus pullStatus,List<Message> msgList) {
    this.pullStatus = pullStatus;
    this.msgList = msgList;
  }


  public PullStatus getPullStatus() {
    return pullStatus;
  }


  public List<Message> getMsgList() {
    return Collections.unmodifiableList(msgList);
  }


}
