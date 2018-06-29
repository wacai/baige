package com.wacai.open.baige.sdk.consumer.atomic;

/**
 * Looper执行状态切换如下：
 * 
 * <pre>
 *                 -----------------
 *                 |               |
 *                \|/              |    
 *      | ----> FETCHING  ---->  WAITING
 *      |          |    \
 *      |          |     \--------------------|
 *      |         \|/                        \|/
 *      |      PROCESSING -------> ABORTED/SUSPENDED
 *      |          |                          ^
 *      |          |    / --------------------|  
 *      |         \|/  /   
 *      |        ACKING   
 *      ^          |    
 *      |          |    
 *      |         \|/   
 *      |       FINISHED
 *      |          |
 *      |          |
 *      -----<------
 * 
 * </pre>
 * 
 * @author cuodao
 */
public enum LooperState {
    /**
     * 等待(服务器端没有消息或未准备好数据)
     */
    WAITING,
    /**
     * 正在跟服务器通讯
     */
    FETCHING,
    /**
     * 正在处理消息
     */
    PROCESSING,
    /**
     * 在向服务器发送ACK
     */
    ACKING,
    /**
     * 完成ACK
     */
    FINISHED,
    
    /**
     * 暂停、服务器端不可用 (线程中止)
     */
    SUSPENDED, 
    
    /**
     * 消息处理时发生异常而导致的中断
     */
    ABORTED;
}