package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.remoting.InvokeCallback;
import com.wacai.open.baige.remoting.common.SemaphoreReleaseOnce;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResponseFuture {

  private volatile RemotingCommand responseCommand;
  private volatile boolean isSendReqOK = true;
  private volatile Throwable cause;
  private int opaque;
  private final long timeoutMills;
  private InvokeCallback invokeCallback;
  private final long beginTimeStamp = System.currentTimeMillis();

  /*用于等异步任务的处理结果*/
  private final CountDownLatch countDownLatch = new CountDownLatch(1);
  private final SemaphoreReleaseOnce semaphoreOnce;
  /*用于控制只执行一次回调函数*/
  private final AtomicBoolean executeCallbackOnce = new AtomicBoolean(false);



  public ResponseFuture(int opaque, long timeoutMills, InvokeCallback invokeCallback, SemaphoreReleaseOnce
       semaphoreOnce) {
    this.opaque = opaque;
    this.timeoutMills = timeoutMills;
    this.invokeCallback = invokeCallback;
    this.semaphoreOnce = semaphoreOnce;
  }

  public void executeInvokeCallback() {
    if (invokeCallback != null) {
      if (executeCallbackOnce.compareAndSet(false ,true)) {
        invokeCallback.operationComplete(this);
      }
    }
  }

  public void release() {
    if (this.semaphoreOnce != null) {
      this.semaphoreOnce.release();
    }
  }

  public boolean isTimeout() {
    long diff = System.currentTimeMillis() - this.beginTimeStamp;
    return diff > timeoutMills;
  }

  public RemotingCommand waitResponse(final long timeoutMills) throws InterruptedException {
    this.countDownLatch.await(timeoutMills, TimeUnit.MILLISECONDS);
    return this.responseCommand;
  }

  public void putResponse(final RemotingCommand responseCommand) {
    this.responseCommand = responseCommand;
    this.countDownLatch.countDown();
  }

  /*getter and setter method*/

  public RemotingCommand getResponseCommand() {
    return responseCommand;
  }

  public void setResponseCommand(RemotingCommand responseCommand) {
    this.responseCommand = responseCommand;
  }

  public int getOpaque() {
    return opaque;
  }

  public long getBeginTimeStamp() {
    return beginTimeStamp;
  }

  public long getTimeoutMills() {
    return timeoutMills;
  }


  public boolean isSendReqOK() {
    return isSendReqOK;
  }

  public void setSendReqOK(boolean sendReqOK) {
    isSendReqOK = sendReqOK;
  }

  public Throwable getCause() {
    return cause;
  }

  public void setCause(Throwable cause) {
    this.cause = cause;
  }

  public InvokeCallback getInvokeCallback() {
    return invokeCallback;
  }
}
