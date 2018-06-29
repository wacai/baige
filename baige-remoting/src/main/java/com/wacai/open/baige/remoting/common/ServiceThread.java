package com.wacai.open.baige.remoting.common;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract  class ServiceThread implements  Runnable{

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceThread.class);
  private static final long JOIN_TIME = 90 * 1000;

  protected final Thread thread;
  private volatile boolean stopped = false;
  private volatile boolean hasNotified = false;
  private volatile boolean suspend = false;
  private long loopIntervalMs = 0;

  private ReentrantLock lock = new ReentrantLock();
  private Condition resumeCondition = lock.newCondition();

  protected ServiceThread(long loopIntervalMs) {
    this.thread =  new Thread(this,  this.getServiceName());
    this.loopIntervalMs = loopIntervalMs;
  }

  protected ServiceThread() {
    this.thread =  new Thread(this,  this.getServiceName());
  }

  public abstract String getServiceName();

  public void start() {
    this.thread.start();
  }

  public void shutdown() {
    this.shutdown(false);
  }

  public void stop() {
    this.stop(false);
  }

  public void stop(boolean interrupt) {
    this.stopped = true;
    LOGGER.info("stop thread " + this.getServiceName() + " interrupt " + interrupt);
    synchronized (this) {
      if (!this.hasNotified) {
        this.hasNotified = true;
        this.notify();
      }
    }

    if (interrupt) {
      this.thread.interrupt();
    }
  }



  public void shutdown(final boolean interrupt) {
    this.stopped = true;
    LOGGER.info("Shutdown thread " + this.getServiceName() + " get interrupt signal");
    synchronized (this) {
      if (!this.hasNotified) {
        this.hasNotified = true;
        this.notify();
      }
    }

    try {
      if (interrupt) {
        this.thread.interrupt();
      }
      long beginTime = System.currentTimeMillis();
      this.thread.join(this.getJoinTime());
      long ellapseTime =  System.currentTimeMillis() - beginTime;
      LOGGER.info("Join thread  " + this.getServiceName() + " ellapse  " + ellapseTime + " ms");

    } catch (InterruptedException e) {
      LOGGER.warn("join thread " + this.getServiceName() + " catch InterruptedException ", e);
    }

  }

  @Override
  public void run() {
    LOGGER.info(this.getServiceName() + " service started");

    while(!isStoped()) {
      if (isSuspend()) {
        LOGGER.info("{} is suspend and wait", getServiceName());
        lock.lock();
        try {
          resumeCondition.await();
        } catch (InterruptedException e) {
          LOGGER.warn("resumeCondition get interrupt signal and exit await");
        } finally {
          lock.unlock();
        }
        LOGGER.info("{} continue to work", getServiceName());
      }
      this.doLoopService();
      if(loopIntervalMs > 0) {
        try {
          Thread.sleep(loopIntervalMs);
        } catch (InterruptedException e) {
          LOGGER.warn("ServiceThread {} sleep {} ms to do another loop service catch Exception",
              this.getServiceName(), loopIntervalMs, e);
        }
      }
    }

    LOGGER.info(this.getServiceName() + " service end");

  }

  /**实际的任务定义*/
  protected abstract void doLoopService();

  public long getJoinTime() {
    return JOIN_TIME;
  }



  public boolean isStoped() {
    return this.stopped;
  }

  public boolean isSuspend() {
    return this.suspend;
  }

  public void suspend() {
    this.suspend = true;
  }

  public void suspend(long suspendTimeMs) {
    this.suspend = true;
    try {
      Thread.currentThread().sleep(suspendTimeMs);
    } catch (InterruptedException e) {
      LOGGER.warn("ServiceThread {} sleep for {} ms catch Exception", getServiceName(), suspendTimeMs, e);
    } finally {
      this.resume();
    }
  }

  public void setLoopInterval(long loopIntervalMs) {
    this.loopIntervalMs = loopIntervalMs;
  }

  public void resume() {
    this.suspend = false;
    lock.lock();
    try {
      resumeCondition.signal();
    } finally {
      lock.unlock();
    }
  }


//  public static void main(String []args) {
//    ServiceThread serviceThread = new ServiceThread(){
//
//      @Override
//      public String getServiceName() {
//        return "test-serviceThread";
//      }
//
//      @Override
//      protected void doLoopService() {
//
//        System.out.println("do loop service and sleep 1 seconds");
//        try {
//          Thread.sleep(1000);
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
//      }
//    };
//    serviceThread.start();
//
//    try {
//      Thread.sleep(5000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//
//    serviceThread.suspend();
//
//    try {
//      Thread.sleep(5000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//
//    serviceThread.resume();
//
//  }
}
