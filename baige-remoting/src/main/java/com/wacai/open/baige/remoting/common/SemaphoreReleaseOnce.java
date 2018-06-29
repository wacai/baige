package com.wacai.open.baige.remoting.common;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class SemaphoreReleaseOnce {

  private final Semaphore semaphore;
  private final AtomicBoolean released = new AtomicBoolean(false);


  public SemaphoreReleaseOnce(Semaphore semaphore) {
    this.semaphore = semaphore;
  }

  public void release() {
    if (semaphore != null) {
      if (this.released.compareAndSet(false, true)) {
        this.semaphore.release();
      }
    }
  }

  /*getter methods*/
  public Semaphore getSemaphore() {
    return semaphore;
  }
}
