package com.wacai.open.baige.sdk;

import com.wacai.open.baige.common.protocol.heartbeat.ConsumerData;
import com.wacai.open.baige.common.protocol.heartbeat.HeartbeatData;
import com.wacai.open.baige.remoting.netty.NettyClientConfig;
import com.wacai.open.baige.sdk.auth.AuthService;
import com.wacai.open.baige.sdk.consumer.AckMessageService;
import com.wacai.open.baige.sdk.consumer.MQConsumerInner;
import com.wacai.open.baige.sdk.exception.ClientException;
import com.wacai.open.baige.sdk.processor.ClientRemotingProcessor;
import com.wacai.open.baige.sdk.producer.MQProducerInner;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MQClientInstance {


  private static final Logger  LOGGER = LoggerFactory.getLogger(MQClientInstance.class);

  private final ClientConfig clientConfig;
  private final int instanceIndex;
  private final String clientId;
  private volatile  ClientState clientState = ClientState.CLOSED;

  private final NettyClientConfig nettyClientConfig;
  private final ClientRemotingProcessor clientRemotingProcessor;
  private MQClientAPIImpl mqClientAPIImpl;
  //  private ConcurrentHashMap<String/*consumer group*/, MQConsumerInner>  consumerTable
//       = new ConcurrentHashMap<>();
  private MQConsumerInner mqConsumerInner;
  private String cosumerGroup;

  private MQProducerInner mqProducerInner;
  private String producerGroup;

  private List<Listener> listeners;

//  private final PullMessageService pullMessageService;

  private final AuthService authService;

  private final AckMessageService ackMessageService;

  private volatile boolean scheduledTaskSuspend = false;




  private final ScheduledExecutorService scheduledExecutorService = Executors
      .newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          return new Thread(r, "MQClientFactoryScheduledThread");
        }
      });


  public MQClientInstance(ClientConfig clientConfig, int instanceIndex, String clientId) {
    this.listeners = Collections.synchronizedList(new LinkedList<>());
    this.clientConfig = clientConfig;
    this.instanceIndex = instanceIndex;
    this.clientId = clientId;
    //    this.pullMessageService = new PullMessageService(this);
    this.authService = new AuthService(clientConfig.getAppSecret(), clientConfig.getAppKey());

    this.ackMessageService = new AckMessageService(this);


    this.nettyClientConfig = new NettyClientConfig();
    this.nettyClientConfig.setClientCallbackExecutorThreads(clientConfig.getClientCallbackExecutorThreads());
    this.clientRemotingProcessor = new ClientRemotingProcessor(this);
    this.mqClientAPIImpl = new MQClientAPIImpl(this.nettyClientConfig,
        this.clientRemotingProcessor, clientConfig , this);

    this.mqClientAPIImpl.registerListener(new MQClientAPIImpl.Listener() {

      @Override
      public void onConnectServerSuccess(String serverAddr, int connectTimes) {
        if (listeners != null && listeners.size() > 0) {
          for (Listener listener : listeners) {
            listener.onConnectServerSuccess(serverAddr, connectTimes);
          }
        }
      }

      @Override
      public void onClose(InetSocketAddress remoteAddr) {
        if (listeners != null && listeners.size() > 0) {
          for (Listener listener : listeners) {
            listener.onClose(remoteAddr);
          }
        }
      }

      @Override
      public void onRemotingError(Throwable t) {
        if (listeners != null && listeners.size() > 0) {
          for (Listener listener : listeners) {
            listener.onRemotingError(t);
          }
        }
      }
    });


  }

  /*断链重连*/
  public Future<Boolean> reconnect(long connectTimeoutMs) throws Exception {
    return this.mqClientAPIImpl.reconnect(connectTimeoutMs);
  }

  public void start() throws ClientException {

    synchronized (this) {
      if (clientState == ClientState.STARTED ) {
        //启动过无需重新启动
        return;
      }
      /*状态检查*/
      clientState = ClientState.STARTING;
      try {
        this.doStart();
      } catch (Exception e) {
        LOGGER.warn("start MQClientInstance catch Exception, clientConfig:{}, clientId:{}",
            clientConfig, clientId, e);
        clientState = ClientState.CLOSED;
        throw new ClientException("start MQClientInstance error, clientConfig:" + this.clientConfig
            + ", clientId:" + this.clientId , e);
      }
      clientState = ClientState.STARTED;

    }


  }

  public void shutdown() {
    synchronized (this) {
      if (clientState != ClientState.STARTED) {
        LOGGER.warn("the state of MQClientInstance is not STARTED, do nothing in shutdown ");
        return;
      }
      this.clientState = ClientState.CLOSING;
      this.mqClientAPIImpl.shutdown();
      this.stopServices();

      MQClientInstanceManager.getInstance().removeMQClientInstance(clientId);
      this.clientState = ClientState.CLOSED;
    }
  }


  public boolean registerProducer(String producerGroup, final MQProducerInner mqProducerInner) {
    if (null == producerGroup || mqProducerInner == null) {
      return false;
    }
    this.producerGroup = producerGroup;
    this.mqProducerInner = mqProducerInner;
    return true;
  }

  public boolean registerConsumer(String consumerGroup, final MQConsumerInner consumer) {
    if (null == consumerGroup || null == consumer) {
      return false;
    }

    /*
    MQConsumerInner prev = this.consumerTable.putIfAbsent(consumerGroup, consumer);
    if (prev != null) {
      LOGGER.warn("the consumergroup[" + consumerGroup + "] exists already");
      return false;
    }*/
    this.cosumerGroup = consumerGroup;
    this.mqConsumerInner = consumer;
    return true;
  }


  public MQConsumerInner selectConsumer(final String group) {
//    return this.consumerTable.get(group);
    return this.mqConsumerInner;
  }


  /*getter and setter method*/
//  public PullMessageService getPullMessageService() {
//    return pullMessageService;
//  }


  public AckMessageService getAckMessageService() {
    return ackMessageService;
  }

  public MQClientAPIImpl getMqClientAPIImpl() {
    return mqClientAPIImpl;
  }


  public AuthService getAuthService() {
    return authService;
  }

  public void registerListener(Listener listener) {
    listeners.add(listener);
  }

  /**
   * 认证通过后， 启动各种服务：拉消息、心跳等。
   */
  public void startServices() {
    if(this.authService.isAuthSuccess()) {
//      this.pullMessageService.start();
      this.ackMessageService.start();
      this.startScheduledTask();
    }

  }

  /**
   * 到服务链接断掉以后 ，需要先暂停掉各种服务；
   */
  public void suspendServices() {
//    if (this.pullMessageService != null) {
//      this.pullMessageService.suspend();
//    }
    if (this.ackMessageService != null) {
      this.ackMessageService.suspend();
    }
    this.suspendScheduledTask();

    if (this.mqClientAPIImpl != null) {
      this.mqClientAPIImpl.suspend();
    }
  }

  public void resumeServices() {
//    if (this.pullMessageService != null) {
//      this.pullMessageService.resume();
//    }
    if (this.ackMessageService != null) {
      this.ackMessageService.resume();
    }
    this.resumeScheduledTask();

    if (this.mqClientAPIImpl != null) {
      this.mqClientAPIImpl.resume();
    }
  }




  public void stopServices() {
//    if (pullMessageService != null) {
//      this.pullMessageService.shutdown(true);
//    }
    if (ackMessageService != null) {
      this.ackMessageService.shutdown();
    }
    this.stopScheduledTask();
  }


  public interface Listener {
    void onConnectServerSuccess(String serverAddr, int connectTimes);
    void onClose(InetSocketAddress remoteAddr);
    void onRemotingError(Throwable t);
  }



  private void doStart() throws Exception {
    this.mqClientAPIImpl.start();

  }


  private void startScheduledTask() {

    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        if (!scheduledTaskSuspend) { //调度任务不支持suspend操作， 用boolean 变量来控制。
//          try {
//            sendHeartbeatToServer();
//          } catch (Exception e) {
//            LOGGER.error("ScheduledTask send heartbeat to broker catch Exception ", e);
//          }
        }
      }
    }, 5000, this.clientConfig.getHeartbeatBrokerInterval(), TimeUnit.MILLISECONDS);


  }

  private void stopScheduledTask() {
    if (this.scheduledExecutorService != null) {
      this.scheduledExecutorService.shutdown();
    }
  }

  private void sendHeartbeatToServer() {
    HeartbeatData heartbeatData = this.prepareHeartbeatData();
    try {
      this.mqClientAPIImpl.sendHeartbeatData(heartbeatData, clientConfig.getHeartbeartSendTimeoutMs());
      LOGGER.info("send heart beat data:{} to wsURI:{}", heartbeatData, this.clientConfig.getWsServerURL());
    } catch (Exception e) {
      LOGGER.error("send heart beat to server :{} exception", this.clientConfig.getWsServerURL(),
          e);
    }


  }



  private HeartbeatData prepareHeartbeatData() {
    HeartbeatData heartbeatData = new HeartbeatData();
    heartbeatData.setClientId(this.clientId);

    /*消费者元数据*/
    ConsumerData consumerData = new ConsumerData();
    consumerData.setGroupName(mqConsumerInner.groupName());
    consumerData.setSubscriptionDataSet(mqConsumerInner.subscriptions());
    heartbeatData.setConsumerData(consumerData);


    return heartbeatData;

  }


  private void suspendScheduledTask() {
    this.scheduledTaskSuspend = true;
  }

  private void resumeScheduledTask() {
    this.scheduledTaskSuspend = false;
  }



  /*getter and setter methods*/

  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  public MQConsumerInner getMqConsumerInner() {
    return mqConsumerInner;
  }
}
