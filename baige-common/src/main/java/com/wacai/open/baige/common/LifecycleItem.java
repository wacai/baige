package com.wacai.open.baige.common;


import com.wacai.open.baige.common.exception.LifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 具有相似的生命周期的对象 ：
 */
public abstract class LifecycleItem {

  protected static final Logger LOGGER = LoggerFactory.getLogger(LifecycleItem.class);

  private LifecycleItemState clientState;

  protected String name;


  public LifecycleItem(String name) {
    this.name = name;
  }

  protected abstract void doStart() throws Exception;

  protected abstract void doStop();




  public void stop() throws LifecycleException {
    synchronized (this) {
      if (clientState != LifecycleItemState.STARTED) {
        return;
      }
      clientState = LifecycleItemState.CLOSING;
      try {
        this.doStop();
      } catch (Exception e) {
        clientState = LifecycleItemState.STARTED;
        throw new LifecycleException("LifecycleItem with name " + this.name + " stop catch Exception",
            e);
      }
      clientState =  LifecycleItemState.CLOSED;
    }
  }

  public void start() throws LifecycleException {
    synchronized (this) {
      /*状态检查*/
      if (clientState == LifecycleItemState.STARTED || clientState == LifecycleItemState.STARTING
          || clientState == LifecycleItemState.CLOSING) {
        throw new LifecycleException("LifecycleItem  can't start as the state is " + clientState,
            null);
      }
      clientState = LifecycleItemState.STARTING;
      try {
        this.doStart();
      } catch (Exception e) {
        clientState = LifecycleItemState.CLOSED;
        throw new LifecycleException("LifecycleItem with name " + this.name + " start catch Exception",
            e);
      }
      clientState = LifecycleItemState.STARTED;

    }

  }
}
