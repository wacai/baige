package com.wacai.open.baige.sdk.producer;

import static com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.AuthFailReason.NOT_PATH_AUTHORIZED;
import static com.wacai.open.baige.common.protocol.authorize.AuthorizeContants.HttpConstants.Headers.AUTH_FAIL_REASON;

import com.wacai.open.baige.common.protocol.ResponseCode;
import com.wacai.open.baige.common.protocol.authorize.AuthResult;
import com.wacai.open.baige.common.protocol.header.SendMsgRequestHeader;
import com.wacai.open.baige.common.protocol.produce.ProduceConstants;
import com.wacai.open.baige.remoting.exception.RemotingException;
import com.wacai.open.baige.sdk.ClientState;
import com.wacai.open.baige.sdk.CommunicationMode;
import com.wacai.open.baige.sdk.MQClientInstance;
import com.wacai.open.baige.sdk.MQClientInstance.Listener;
import com.wacai.open.baige.sdk.MQClientInstanceManager;
import com.wacai.open.baige.sdk.consumer.ConsumeMessageService;
import com.wacai.open.baige.sdk.consumer.DefaultMQConsumerInnerImpl;
import com.wacai.open.baige.sdk.consumer.atomic.MQAtomConsumer;
import com.wacai.open.baige.sdk.exception.AuthException;
import com.wacai.open.baige.sdk.exception.ClientException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import jdk.internal.dynalink.support.DefaultPrelinkFilter;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMQProducerInnerImpl implements MQProducerInner {

  private static Logger LOGGER = LoggerFactory.getLogger(DefaultMQProducerInnerImpl.class);

  private DefaultMQProducer defaultMQProducer;

  private MQClientInstance mqClientInstance;

  private volatile ClientState clientState = ClientState.CLOSED;

  /*是否做断链重连*/
  private AtomicBoolean isReconnect = new AtomicBoolean(true);

  public DefaultMQProducerInnerImpl(DefaultMQProducer defaultMQProducer) {
    this.defaultMQProducer = defaultMQProducer;
    this.mqClientInstance = MQClientInstanceManager.getInstance().getMQClientInstance(this.defaultMQProducer);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        isReconnect.set(false);
      }
    });

  }


  @Override
  public void start() throws ClientException, AuthException {

    synchronized (this) {
      if (clientState == ClientState.STARTED) {
        //启动过无需重新启动
        return;
      }
      this.clientState = ClientState.STARTING;

      this.mqClientInstance.registerProducer(this.defaultMQProducer.getProducerGroup(),
          this);

      this.mqClientInstance.registerListener(new Listener() {
        @Override
        public void onConnectServerSuccess(String serverAddr, int connectTimes) {
          DefaultMQProducerInnerImpl.LOGGER.info("DefaultMQProducerInnerImpl connect to {} successfully",
              serverAddr);

          mqClientInstance.getAuthService().authSuccess();

        }

        @Override
        public void onClose(InetSocketAddress remoteAddr) {

          if (!isReconnect.get()) {
            LOGGER.info(" isReconnect is set to false by jvm shutdown hook, so mqClientInstance don't need to reconnect to server");
            return;
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
            LOGGER.info("It's {} time to  try to reconnect to {}", ++reconnectCount, defaultMQProducer.getWsServerURL());
            try {
              connFuture = DefaultMQProducerInnerImpl.this.mqClientInstance.reconnect(defaultMQProducer.getConnectTimeousMs());
              continueReconnect = connFuture.get() ? false : true; //重连成功， 则不继续重连；
            } catch (Exception e) {
              continueReconnect = true; //发生重连异常，则sleep一段时间，再次尝试重连
              LOGGER.warn("reconnect to {} catch Exception", defaultMQProducer.getWsServerURL(), e);
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
        authResult = this.mqClientInstance.getAuthService().waitAuthResult(this.defaultMQProducer.getAuthorizeTimeoutMs());
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
        this.mqClientInstance.startScheduledTask();
      }

      this.clientState = ClientState.STARTED;


    } //end of synchronized block

  }

  @Override
  public void shutdown() {
    synchronized (this) {
      LOGGER.info("DefaultMQProducerInnerImpl shutdown");
      this.clientState = ClientState.CLOSING;
      this.mqClientInstance.shutdown();
      this.clientState = ClientState.CLOSED;
    }

  }

  @Override
  public void send(String topic, String msgBody, SendCallback sendCallback, long timeoutMills)
      throws ClientException, RemotingException, InterruptedException {
    if (topic == null) {
      throw new ClientException(ResponseCode.MESSAGE_ILLEGAL, "the topic is null");
    }

    if (msgBody == null) {
      throw new ClientException(ResponseCode.MESSAGE_ILLEGAL, "the msgBody is null");
    }

    if (msgBody.length() == 0) {
      throw new ClientException(ResponseCode.MESSAGE_ILLEGAL, "the msgBody length is 0 ");
    }

    if (msgBody.length() > ProduceConstants.MAX_BODY_SIZE_IN_BYTES) {
      throw new ClientException(ResponseCode.MESSAGE_ILLEGAL, "the max msgBody length is "  + ProduceConstants.MAX_BODY_SIZE_IN_BYTES);
    }

    SendMsgRequestHeader sendMsgRequestHeader = new SendMsgRequestHeader();
    sendMsgRequestHeader.setTopic(topic);
    sendMsgRequestHeader.setProducerGroup(defaultMQProducer.getProducerGroup());
    this.mqClientInstance.getMqClientAPIImpl().sendMessage(sendMsgRequestHeader, msgBody, timeoutMills, CommunicationMode.ASYNC, sendCallback);


  }




}
