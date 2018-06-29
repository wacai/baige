package com.wacai.open.baige.sdk.consumer.atomic;

/**
 * 消费端工作线程， 以轮询方式从服务端获取消息并处理。
 */
public class Looper  extends Thread{

//  private static final Logger LOGGER = LoggerFactory.getLogger(Looper.class);
//
//  private Message message;
//
//  protected final Map<LooperState, StateAction> actions = new HashMap<>();
//
//  private volatile LooperState currentState = LooperState.WAITING;
//
//
//  public Looper() {
//    this.initActions();
//
//  }
//
//
//  /**
//   * 执行每个状态对应的行为并返回下一状态
//   */
//  protected interface StateAction {
//    LooperState perform() throws Exception;
//  }
//
//  private void initActions() {
//    actions.put(LooperState.WAITING, new StateAction(){
//
//      @Override
//      public LooperState perform() throws Exception {
//        try {
//          Thread.sleep(1000);
//        } catch (InterruptedException ie) {
//          LOGGER.warn("sleep catch Exception", ie);
//        }
//        return LooperState.FETCHING;
//      }
//    });
//
//    actions.put(LooperState.FETCHING, new StateAction() {
//      @Override
//      public LooperState perform() throws Exception {
//        long t1 = System.currentTimeMillis();
//      }
//    });
//  }


}
