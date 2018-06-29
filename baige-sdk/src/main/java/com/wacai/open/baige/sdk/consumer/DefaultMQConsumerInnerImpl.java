package com.wacai.open.baige.sdk.consumer;

import static com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.AuthFailReason.NOT_PATH_AUTHORIZED;
import static com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.HttpConstants.Headers.AUTH_FAIL_REASON;

import com.wacai.open.baige.common.exception.LifecycleException;
import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.common.protocol.authorize.AuthResult;
import com.wacai.open.baige.common.protocol.header.PullMessageRequestHeader;
import com.wacai.open.baige.common.protocol.heartbeat.SubscriptionData;
import com.wacai.open.baige.common.util.CollectionUtil;
import com.wacai.open.baige.remoting.netty.websocket.client.WebSocketRemotingClient;
import com.wacai.open.baige.sdk.ClientState;
import com.wacai.open.baige.sdk.CommunicationMode;
import com.wacai.open.baige.sdk.MQClientInstance;
import com.wacai.open.baige.sdk.MQClientInstance.Listener;
import com.wacai.open.baige.sdk.MQClientInstanceManager;
import com.wacai.open.baige.sdk.consumer.atomic.MQAtomConsumer;
import com.wacai.open.baige.sdk.consumer.listener.MessageListener;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 这个类负责给SDK的用户提供服务。
 *
 */
public class DefaultMQConsumerInnerImpl implements  MQConsumerInner  {

  private static Logger LOGGER = LoggerFactory.getLogger(DefaultMQConsumerInnerImpl.class);

  /*一次拉消息遇到异常以后，延时多长时间重新拉取，默认：3秒*/
  private static final long PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION = 3000;

  private static final long PULL_TIME_DELAY_MILLS_WHEN_NOT_FOUND = 3000;

  private static final long ACK_TIME_DELAY_MILLS_WHEN_EXCEPTION = 3000;


  /*一次拉消息的超时时间 */
  private static final long CONSUMER_TIMEOUT_MILLLS = 1000 * 3;


  private final DefaultMQConsumer defaultMQConsumer;

  private volatile  ClientState clientState = ClientState.CLOSED;

  private MQClientInstance mqClientInstance;

  private ConcurrentHashMap<String/*topic*/, MessageListener> topicListeners;

  private ConcurrentHashMap<String/*topic*/, SubscriptionData> subscriptionDatas;

  private Set<MQAtomConsumer> mqAtomConsumers;



  private ConsumeMessageService consumeMessageService;








  public DefaultMQConsumerInnerImpl(DefaultMQConsumer defaultMQConsumer) {
    this.defaultMQConsumer =  defaultMQConsumer;
    this.topicListeners = new ConcurrentHashMap<>();
    this.subscriptionDatas = new ConcurrentHashMap<>();
    this.mqAtomConsumers = Collections.synchronizedSet(new HashSet<>());

    this.mqClientInstance = MQClientInstanceManager.getInstance().getMQClientInstance(this.defaultMQConsumer);


  }

  public void start() throws ClientException, AuthException, LifecycleException {
    synchronized (this) {
      /*状态检查*/
      if (clientState == ClientState.STARTED || clientState == ClientState.STARTING
          || clientState == ClientState.CLOSING) {
        LOGGER.warn("The defaultMQConsumer can't start as the clientState is  {} ", clientState);
        throw new ClientException("The defaultMQConsumer can't start as the clientState is " +
            clientState, null);
      }
      this.clientState = ClientState.STARTING;
      /*配置检查*/
//      this.checkConfig();


      boolean  isRegisterConsumerOK = this.mqClientInstance.registerConsumer(
          this.defaultMQConsumer.getConsumerGroup(),this);
      if (!isRegisterConsumerOK) {
        this.clientState =  ClientState.CLOSED;
        throw new ClientException("The consumer group [" + this.defaultMQConsumer.getConsumerGroup()
            + " ] has been created before, specify another name please", null);
      }


      this.mqClientInstance.registerListener(new Listener() {
        @Override
        public void onConnectServerSuccess(String serverAddr, int connectTimes) {
          DefaultMQConsumerInnerImpl.LOGGER.info(
              "DefaultMQConsumerInnerImpl   connect to {} successfully", serverAddr);

          /**{@link WebSocketRemotingClient#start()} 启动函数中已经发送了授权信息， 所以这里一旦连接成功，
           * 则认证通过*/
          mqClientInstance.getAuthService().authSuccess();

          /*断链重连成功以后，打开被暂停的服务*/
          if (connectTimes > 1) {
            LOGGER.info("resume the DefaultMQConsumerInnerImpl'service after {} times connecting to [wsURL:{},serverAddr:{}] successfully",
                connectTimes, defaultMQConsumer.getWsServerURL(), serverAddr);
            DefaultMQConsumerInnerImpl.this.resume();
          }
        }

        @Override
        public void onClose(InetSocketAddress remoteAddr) { //和服务器的链路断链以后，先暂停各种服务， 然后尝试进行断链重连；
          try {
            /*暂停服务*/
            DefaultMQConsumerInnerImpl.this.suspend();
          } catch (Exception e) {
            LOGGER.warn("DefaultMQConsumerInnerImpl recv onClose event , but suspend() catch Exception, remoteAddr:{}",
                remoteAddr, e );
          }
          boolean continueReconnect = false;
          int reconnectCount = 0;
          /*尝试进行断链重连*/
          Future<Boolean> connFuture = null;
          for(;;) {
            if (ClientState.STARTED != clientState) {
              LOGGER.info("the clientState is not STARTED, exit for reconnecting to server ");
              break;
            }
            LOGGER.info("{} try to reconnect to {}", ++reconnectCount, defaultMQConsumer.getWsServerURL());
            try {
              connFuture = DefaultMQConsumerInnerImpl.this.mqClientInstance.reconnect(defaultMQConsumer.getConnectTimeousMs());
              continueReconnect = connFuture.get() ? false : true; //重连成功， 则不继续重连；
            } catch (Exception e) {
              continueReconnect = true; //发生重连异常，则sleep一段时间，再次尝试重连
              LOGGER.warn("reconnect to {} catch Exception", defaultMQConsumer.getWsServerURL(), e);
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e1) {
                LOGGER.warn("sleep 5000 ms for next reconnect catch InterruptedException");
              }
            }
            if (!continueReconnect) {
              LOGGER.info("the continueReconnect is false, exit for reconnecting to server  ");
              break;  //break for loop
            }
          }
        }

        @Override
        public void onRemotingError(Throwable t) {
          if (t instanceof UpgradeException) {
            UpgradeException upgradeException  = (UpgradeException)t;
            if (upgradeException.getResponseStatusCode() == 401) {
              //启动时收到401 status code， 授权认证不通过；
              String authFailReason = null;
              try {
                authFailReason = ((HttpResponseException)upgradeException.getCause()).getResponse().getHeaders().get(AUTH_FAIL_REASON);
              } catch (Exception e) {
                authFailReason = NOT_PATH_AUTHORIZED;
              }
              mqClientInstance.getAuthService().authFailed(authFailReason);
            }
          }
        }
      });
      this.mqClientInstance.start();

      AuthResult authResult = null;
      try {
        authResult = this.mqClientInstance.getAuthService().waitAuthResult(this.defaultMQConsumer.getAuthorizeTimeoutMs());
      } catch (InterruptedException e) {
        LOGGER.error("wait authorize result's thread is interrupted ", e);
        throw new AuthException("wait authorize result's thread is interrupted", e);
      } catch (AuthException e) {
        LOGGER.error("authorize catch exception, reason:{}", e.getErrorMessage(), e);
        throw new AuthException(e.getErrorMessage(), e);
      }
      if (!this.mqClientInstance.getAuthService().isAuthSuccess()) {
        /*授权认证不通过*/
        LOGGER.warn("not pass authorize, reason is {}", authResult.getFailReason());
        throw new AuthException(authResult.getFailReason(), null);
      } else {
        /*授权认证通过，启动各种服务*/
        LOGGER.info("pass authorize and start other services");
        this.mqClientInstance.startServices();

        /*启动每个topic的消费。*/
        for (MQAtomConsumer mqAtomConsumer : mqAtomConsumers) {
          mqAtomConsumer.start();
        }

        //启动消息的消费服务；
        this.consumeMessageService = new ConsumeMessageService(defaultMQConsumer, this);
        this.consumeMessageService.start();


      }
      this.clientState = ClientState.STARTED;


    }

  }

  public void shutdown() {
    synchronized (this) {
//      if (this.clientState != ClientState.STARTED) {
//        LOGGER.warn("The state of DefaultMQConsumer is not started , do nothing in shutdown");
//        return;
//      }
      LOGGER.info("DefaultMQConsumerInnerImpl shutdown");
      this.clientState = ClientState.CLOSING;
      this.mqClientInstance.shutdown();
      if (this.consumeMessageService != null) {
        this.consumeMessageService.shutdown();
      }
      this.clientState = ClientState.CLOSED;
    }
  }



  public DefaultMQConsumerInnerImpl registerMessageListener(String topic, MessageListener messageListener) {
    MessageListener oldListener = this.topicListeners.putIfAbsent(topic, messageListener);
    if (oldListener == null) {
      SubscriptionData subscriptionData = new SubscriptionData(topic);
      this.subscriptionDatas.putIfAbsent(topic, subscriptionData);

      MQAtomConsumer mqAtomConsumer = new MQAtomConsumer(
          this.defaultMQConsumer.getConsumerGroup(),
          this.defaultMQConsumer.getConsumerID(),
          topic,
          messageListener,
          mqClientInstance,
          this.defaultMQConsumer.getPullTimeIntervalMs(),
          this.defaultMQConsumer.getPullThreadsNum()
      );
      mqAtomConsumer.setMessageListener(messageListener);
      mqAtomConsumer.setMQConsumerInner(this);
      mqAtomConsumer.setPullIntervalMs(this.defaultMQConsumer.getPullTimeIntervalMs());


      this.mqAtomConsumers.add(mqAtomConsumer);



    }
    return this;
  }

//  public DefaultMQConsumerInnerImpl registerMessageListener(String topic, MessageListener messageListener) {
//
//  }


  @Override
  public void executeAckRequestNow(AckRequest ackRequest) {
    this.mqClientInstance.getAckMessageService().executeAckRequestNow(ackRequest);
  }

  @Override
  public boolean resumePullMsg(String consumerGroup, String topic, List<Message> messageList) {
    for(MQAtomConsumer mqAtomConsumer : mqAtomConsumers ) { //选择对应的mqAtomConsumer 恢复消息的拉取
      if (consumerGroup.equals(mqAtomConsumer.getConsumerGroup()) && topic.equals(mqAtomConsumer.getTopic())) {
        return mqAtomConsumer.resumePullMsg(messageList);
      }
    }
    return false;
  }

  @Override
  public void recvMessageList(PullRequest pullRequest, List<Message> messages) {
    PullResult pullResult = new PullResult(PullStatus.FOUND, messages);
    consumeMessageService.submitConsumeRequest(pullRequest, pullResult, pullRequest.getMessageListener());
  }


  public void executeAckRequestLater(final AckRequest ackRequest, final long timeDelayMills) {
    this.mqClientInstance.getAckMessageService().executeAckRequestLater(ackRequest, timeDelayMills);
  }



  @Override
  public void doAckMsg(final AckRequest ackRequest) {

    try {
      this.mqClientInstance.getMqClientAPIImpl().ackMessage(
          ackRequest,
          this.defaultMQConsumer.getAckTimeoutMs(),
          new AckMsgCallback() {
            @Override
            public void onSuccess(AckRequest ackRequest) {
//              PullRequest pullRequest = new PullRequest(
//                  ackRequest.getConsumerGroup(),
//                  ackRequest.getConsumerId(),
//                  ackRequest.getTopic(),
//                  ackRequest.getMaxConsumeRetries(),
//                  ackRequest.getMessageListener()
//              );
              LOGGER.info("ack msg success, ackRequest: {}", ackRequest);
            }

            @Override
            public void onException(AckRequest ackRequest, Throwable t) {
              LOGGER.error("use MQClientAPIImpl to ack {} ] catch Exception", ackRequest, t);
              DefaultMQConsumerInnerImpl.this.executeAckRequestLater(ackRequest, ACK_TIME_DELAY_MILLS_WHEN_EXCEPTION);
            }
          }

      );
    } catch (Exception e) {
      LOGGER.error("use MQClientAPIImpl to ack {} ] catch Exception", ackRequest, e);
      DefaultMQConsumerInnerImpl.this.executeAckRequestLater(ackRequest, ACK_TIME_DELAY_MILLS_WHEN_EXCEPTION);
    }
  }



  @Override
  public void doPullMessage(PullRequest pullRequest, PullMsgCallback pmc) {

    PullMessageRequestHeader requestHeader =  new PullMessageRequestHeader();
    requestHeader.setThreadId(Thread.currentThread().getName());
    requestHeader.setTopic(pullRequest.getTopic());


    PullMsgCallback pullMsgCallback = new PullMsgCallback() {
      @Override
      public void onSuccess(PullResult pullResult) {
        switch (pullResult.getPullStatus()) {
          case FOUND:
            //提交消费服务
            DefaultMQConsumerInnerImpl.this.consumeMessageService.submitConsumeRequest(
                pullRequest,
                pullResult,
                pullRequest.getMessageListener()
            );
            //通知回调函数消息拉取成功
            pmc.onSuccess(pullResult);
            break;
          case PULL_NOT_FOUND:
            //通知回调函数消息未拉取到；
            LOGGER.info("pull msg but not found , pull status is :{} ", pullResult.getPullStatus());
            pmc.onMsgNotFound(pullRequest);
            break;
          default:
            if (pullResult != null && pullResult.getPullStatus() != null) {
              LOGGER.warn("pull msg  get  pull status is :{} ", pullResult.getPullStatus());
            }
            break;
        }

      }

      @Override
      public void onException(Throwable e) {
        LOGGER.error("use MQClientAPIImpl to pull msg {} catch Exception", pullRequest, e);
//        DefaultMQConsumerInnerImpl.this.executePullRequestLater(pullRequest,
//            PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION);
        pmc.onException(e);
      }

      @Override
      public void onMsgNotFound(PullRequest pullRequest) {
        pmc.onMsgNotFound(pullRequest);
      }
    };

    try {
      PullResult pullResult = this.mqClientInstance.getMqClientAPIImpl().pullMessage(
          null, requestHeader, defaultMQConsumer.getPullTimeoutMs(), CommunicationMode.ASYNC, pullMsgCallback);

    } catch (Exception e) {
      LOGGER.error("use MQClientAPIImpl to pull msg {} catch Exception", pullRequest, e);
//      DefaultMQConsumerInnerImpl.this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION);
      pmc.onException(e);
    }


  }





  private void checkConfig() throws ClientException {
    if (this.defaultMQConsumer.getAppKey() == null || this.defaultMQConsumer.getAppSecret() == null ||
        this.defaultMQConsumer.getWsServerURL() == null || this.defaultMQConsumer.getConsumerGroup() == null) {
      throw new ClientException("the appKey , appSecret, serverURL, consumerGroup of DefaultMQConsumer"
          + " can not be null", null);
    }

    if (CollectionUtil.isEmpty(topicListeners)) {
      throw new ClientException("The DefaultMQConsumer has no topic config, please config it first"
          , null);
    }
  }

  private void checkState() throws ClientException {
    if (this.clientState != ClientState.STARTED) {
      throw new ClientException("The DefaultMQConsumerInnerImpl's state is not ClientState.STARTED ", null);

    }
  }



  @Override
  public String groupName() {
    return this.defaultMQConsumer.getConsumerGroup();
  }

  @Override
  public Set<SubscriptionData> subscriptions() {
    Set<SubscriptionData> subSet =  new HashSet<>();
    subSet.addAll(this.subscriptionDatas.values());
    return subSet;
  }


  public void suspend() {
    this.mqClientInstance.suspendServices();
    if (mqAtomConsumers != null && mqAtomConsumers.size() > 0) {
      for (MQAtomConsumer mqAtomConsumer : mqAtomConsumers) {
        mqAtomConsumer.suspend();
      }
    }

  }

  public void resume() {
    this.mqClientInstance.resumeServices();

    if (mqAtomConsumers != null && mqAtomConsumers.size() > 0) {
      for (MQAtomConsumer mqAtomConsumer : mqAtomConsumers) {
        mqAtomConsumer.resume();
      }
    }
  }


}
