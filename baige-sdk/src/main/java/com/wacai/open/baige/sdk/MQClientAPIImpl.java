package com.wacai.open.baige.sdk;

import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.common.message.MessageCodec;
import com.wacai.open.baige.common.protocol.RequestCode;
import com.wacai.open.baige.common.protocol.ResponseCode;
import com.wacai.open.baige.common.protocol.header.AckMessageRequestHeader;
import com.wacai.open.baige.common.protocol.header.AckMessageResponseHeader;
import com.wacai.open.baige.common.protocol.header.PullMessageRequestHeader;
import com.wacai.open.baige.common.protocol.header.PullMessageResponseHeader;
import com.wacai.open.baige.common.protocol.header.SendMsgRequestHeader;
import com.wacai.open.baige.common.protocol.heartbeat.HeartbeatData;
import com.wacai.open.baige.remoting.InvokeCallback;
import com.wacai.open.baige.remoting.RemotingClient;
import com.wacai.open.baige.remoting.exception.RemotingCommandException;
import com.wacai.open.baige.remoting.exception.RemotingException;
import com.wacai.open.baige.remoting.listener.RemotingClientListener;
import com.wacai.open.baige.remoting.netty.NettyClientConfig;
import com.wacai.open.baige.remoting.netty.ResponseFuture;
import com.wacai.open.baige.remoting.netty.websocket.client.WebSocketRemotingClient;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import com.wacai.open.baige.sdk.consumer.AckMsgCallback;
import com.wacai.open.baige.sdk.consumer.AckRequest;
import com.wacai.open.baige.sdk.consumer.PullMsgCallback;
import com.wacai.open.baige.sdk.consumer.PullResult;
import com.wacai.open.baige.sdk.consumer.PullStatus;
import com.wacai.open.baige.sdk.exception.AckMsgException;
import com.wacai.open.baige.sdk.exception.ClientException;
import com.wacai.open.baige.sdk.exception.ServerException;
import com.wacai.open.baige.sdk.processor.ClientRemotingProcessor;
import com.wacai.open.baige.sdk.producer.SendCallback;
import com.wacai.open.baige.sdk.producer.SendResult;
import com.wacai.open.baige.sdk.producer.SendStatus;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 发送和处理Request 都在这个类处理。
 */
public class MQClientAPIImpl {

  private static Logger LOGGER = LoggerFactory.getLogger(MQClientAPIImpl.class);

  private final RemotingClient remotingClient;
  private final ClientRemotingProcessor clientRemotingProcessor;

  private List<Listener> listeners;
  private MQClientInstance mqClientInstance;





  public MQClientAPIImpl(NettyClientConfig nettyClientConfig, final ClientRemotingProcessor
      clientRemotingProcessor, ClientConfig clientConfig , MQClientInstance mqClientInstance) {
    this.listeners = new LinkedList<>();
//    this.remotingClient = new NettyRemotingClient(nettyClientConfig, null);
    /*替换成 websocket  client, 和NettyRemotingClient做成两套
     * NettyRemotingClient  专门用于socket通信；  */
    this.remotingClient = new WebSocketRemotingClient(
        nettyClientConfig,
        clientConfig.getWsServerURL(),
        mqClientInstance.getAuthService().getAuthorizeData()
    );

    this.clientRemotingProcessor = clientRemotingProcessor;

    //this.remotingClient.registerRPCHook(rpcHook);

    /**
     注册request code 处理器（方向： server-> client).
     */
    this.remotingClient.registerProcessor(RequestCode.GET_CLIENT_META_INFO, this.clientRemotingProcessor, null);
    this.remotingClient.registerProcessor(RequestCode.RESUME_PULL_MSG, this.clientRemotingProcessor, null);

    this.remotingClient.registerListener(new RemotingClientListener() {
      @Override
      public void onConnect(String serverAddr, int connectTimes) {
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

    this.mqClientInstance = mqClientInstance;


  }


  public void start() throws Exception {
    this.remotingClient.start();
  }

  public void shutdown() {
    this.remotingClient.shutdown();
  }


  public void suspend() {
    this.remotingClient.suspend();
  }

  public void resume() {
    this.remotingClient.resume();

  }


  public void ackMessage(final AckRequest ackRequest, long ackTimeoutMs, final AckMsgCallback ackMsgCallback)
  throws RemotingException, InterruptedException{
    AckMessageRequestHeader requestHeader = new AckMessageRequestHeader();
    requestHeader.setThreadId(Thread.currentThread().getName());
    requestHeader.setOffset(ackRequest.getOffset());


    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.ACK_MSG, requestHeader, ackRequest.getTopic());

    this.remotingClient.invokeAsync(null, request, ackTimeoutMs, new InvokeCallback() {
      @Override
      public void operationComplete(ResponseFuture responseFuture) {
        /*接收到服务端的response*/
        RemotingCommand response = responseFuture.getResponseCommand();
        if (response != null) {
          try {
            AckMessageResponseHeader responseHeader =
                (AckMessageResponseHeader) response.decodeCommandCustomHeader(AckMessageResponseHeader.class);
            if (responseHeader != null && responseHeader.isAckSuccess()) {
              ackMsgCallback.onSuccess(ackRequest);
            } else {
              ackMsgCallback.onException(ackRequest, new AckMsgException(responseHeader.getAckFailReason()));
            }
          }  catch (Exception e) {
            ackMsgCallback.onException(ackRequest, e);
          }
        } else {
          if (!responseFuture.isSendReqOK()) {
            ackMsgCallback.onException(ackRequest, new ClientException("send request failed", responseFuture.getCause()));
          } else if (responseFuture.isTimeout()) {
            ackMsgCallback.onException(ackRequest, new ClientException("wait response timeout " + responseFuture.getTimeoutMills() + "ms",
                responseFuture.getCause()));
          } else {
            ackMsgCallback.onException(ackRequest, new ClientException("unknow reseaon", responseFuture.getCause()));
          }
        }
      }
    });


  }

  public SendResult sendMessage(final SendMsgRequestHeader sendMsgRequestHeader, final String msgBody,
      final long timeoutMills, final CommunicationMode communicationMode, final SendCallback sendCallback)
      throws RemotingException, InterruptedException {
    String topic = sendMsgRequestHeader.getTopic();
    sendMsgRequestHeader.setTopic(null);
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.PUSH_MSG, sendMsgRequestHeader, topic);
    try {
      request.setBody(msgBody.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOGGER.warn("encode msg body catch Exception", e);
    }

    switch (communicationMode) {
      case ONEWAY:
        assert false;
        return null;
      case ASYNC:
        this.sendMessageAsync(request, timeoutMills, sendCallback);
        return null;
      case SYNC:
        assert  false;
        return null;
      default:
        assert false;
        break;
    }

    return null;


  }

  private void sendMessageAsync(RemotingCommand request, long timeoutMills, SendCallback sendCallback)
      throws InterruptedException, RemotingException{
    this.remotingClient.invokeAsync(null, request, timeoutMills, new InvokeCallback() {
      @Override
      public void operationComplete(ResponseFuture responseFuture) {
        RemotingCommand response = responseFuture.getResponseCommand();
//        LOGGER.info("the opaque of send msg response is {}", response.getOpaque());
        if (response != null) {
          try {
            SendResult sendResult = MQClientAPIImpl.this.processSendMsgResponse(response);
            sendCallback.onSuccess(sendResult);
          } catch (Exception e) {
            sendCallback.onException(e);
          }
        } else {
          if (!responseFuture.isSendReqOK()) {
            sendCallback.onException(new ClientException("send push msg request failed", responseFuture.getCause()));
          } else if (responseFuture.isTimeout()) {
            sendCallback.onException(new ClientException("wait response timeout " + responseFuture.getTimeoutMills() + "ms",
                responseFuture.getCause()));
          } else {
            sendCallback.onException(new ClientException("unknow reason", responseFuture.getCause()));
          }
        }
      }
    });
  }


  /**
   *
   * @param serverAddr 如果是socket服务器地址, 则serverAddr的格式是：  ip:port
   *                   如果是websocket服务器地址，则此参数可以不填。
   * @param requestHeader 拉消息请求头
   * @param timeoutMills 拉消息超时时间
   * @param communicationMode 拉消息模式。
   * @param pullMsgCallback 拉消息回调函数。
   * @return PullResult PullResult
   * @throws RemotingException RemotingException
   * @throws InterruptedException  InterruptedException
   */
  public PullResult pullMessage(final String serverAddr, final PullMessageRequestHeader requestHeader, final long
      timeoutMills, final CommunicationMode communicationMode, final PullMsgCallback pullMsgCallback)
      throws RemotingException, InterruptedException {
    String topic = requestHeader.getTopic();
    requestHeader.setTopic(null);
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.PULL_MSG, requestHeader, topic);

    switch (communicationMode) {
      case ONEWAY:
        assert false;
        return null;
      case ASYNC:
        this.pullMessageAsync(serverAddr, request, timeoutMills, pullMsgCallback);
        return null;
      case SYNC:
        return this.pullMessageSync(serverAddr, request, timeoutMills);
      default:
        assert false;
        break;
    }

    return null;
  }

  public void sendHeartbeatData(final HeartbeatData heartbeatData, long sendTimeoutMs)
      throws InterruptedException, RemotingException, ServerException {
    RemotingCommand heartbeatCommand = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);

    heartbeatCommand.setBody(heartbeatData.encode());
    RemotingCommand response = this.remotingClient.invokeSync(null, heartbeatCommand, sendTimeoutMs);
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        return;
      default:
        break;
    }
    throw new ServerException(response.getCode(), response.getRemark());
  }

  public void sendKeepAliveHeartbeatData(long keepaliveHeartbeatSendTimeoutMs) throws Exception {
    this.remotingClient.sendKeepaliveHeartBeat(keepaliveHeartbeatSendTimeoutMs);
  }



  private PullResult pullMessageSync(String serverAddr, RemotingCommand request,
      long timeoutMills) {
    return null;
  }

  private void pullMessageAsync(final String serverAddr, RemotingCommand request,
      long timeoutMills,  PullMsgCallback pullMsgCallback) throws RemotingException, InterruptedException {
    this.remotingClient.invokeAsync(serverAddr, request, timeoutMills, new InvokeCallback() {
      @Override
      public void operationComplete(ResponseFuture responseFuture) {
        /*接收到服务端的response*/
        RemotingCommand response = responseFuture.getResponseCommand();
        if (response != null) {
          try {
             PullResult pullResult = MQClientAPIImpl.this.processPullResponse(response);
            pullMsgCallback.onSuccess(pullResult);
          }  catch (Exception e) {
             pullMsgCallback.onException(e);
          }
        } else {
           if (!responseFuture.isSendReqOK()) {
            pullMsgCallback.onException(new ClientException("send request failed,request opaque:"+request.getOpaque(), responseFuture.getCause()));
          } else if (responseFuture.isTimeout()) {
            pullMsgCallback.onException(new ClientException("request opaque:"+request.getOpaque()+",wait response timeout " + responseFuture.getTimeoutMills() + "ms",
                responseFuture.getCause()));
          } else {
            pullMsgCallback.onException(new ClientException("request opaque:"+request.getOpaque()+",unknow reason", responseFuture.getCause()));
          }
        }
      }
    });
  }

  private SendResult processSendMsgResponse(RemotingCommand response) throws ServerException {

    SendStatus sendStatus = null;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        sendStatus = SendStatus.OK;
        break;
      case ResponseCode.SYSTEM_ERROR:
        sendStatus = SendStatus.FAILURE;
        break;
      case ResponseCode.TOPIC_NOT_EXIST:
        sendStatus = SendStatus.TOPIC_NOT_EXIST;
        break;
      case ResponseCode.TOPIC_NOT_AUTHORIZED:
        sendStatus = SendStatus.TOPIC_NOT_AUTHORIZED;
        break;
      case ResponseCode.MSG_BODY_EXCEED_LIMIT:
        sendStatus = SendStatus.MSG_BODY_EXCEED_LIMIT;
        break;
      default:
        throw new ServerException(response.getCode(), response.getRemark());
    }

    SendResult sendResult = new SendResult();
    sendResult.setSendStatus(sendStatus);
    return sendResult;
  }

  private PullResult processPullResponse(RemotingCommand response)
      throws ServerException, RemotingCommandException {
    PullStatus pullStatus = PullStatus.NO_NEW_MSG;
     switch (response.getCode()) {
      case ResponseCode.PULL_NOT_FOUND: {
        return new PullResult(PullStatus.PULL_NOT_FOUND);
      }
    case ResponseCode.TOPIC_NOT_ACK: {
        return new PullResult(PullStatus.TOPIC_NOT_ACK);
    }
      case ResponseCode.AUTH_SYS_ERROR: {
        return new PullResult(PullStatus.AUTH_SYS_ERROR);
//        throw new ServerException(response.getCode(), "auth sys error");
      }
      case ResponseCode.CLIENT_NOT_AUTHORIZED: {
        return new PullResult(PullStatus.NOT_AUTHORIZED);
//        throw new ServerException(response.getCode(), "client not authorized");
      }
      case ResponseCode.TOPIC_NOT_AUTHORIZED: {
        return new PullResult(PullStatus.TOPIC_NOT_AUTHORIZED);
//        throw new ServerException(response.getCode(), "topic not authorized");
      }
      case ResponseCode.SUCCESS:
        pullStatus = PullStatus.FOUND;
        break;

      default:
        throw new ServerException(response.getCode(), response.getRemark());

    }

    PullMessageResponseHeader responseHeader =
        (PullMessageResponseHeader) response.decodeCommandCustomHeader(PullMessageResponseHeader.class);


    List<Message> messages = null;
    if (pullStatus == PullStatus.FOUND) {
      messages =  MessageCodec.decode(response.getBody());
    }
    PullResult pullResult = new PullResult(pullStatus, messages);

    return pullResult;
  }


  public void registerListener(Listener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public Future<Boolean> reconnect(long connectTimeoutMs) throws Exception {
    return this.remotingClient.reconnect(connectTimeoutMs);
  }

  public interface Listener {
    void onConnectServerSuccess(String serverAddr, int connectTimes);
    void onClose(InetSocketAddress remoteAddr);
    void onRemotingError(Throwable t);

  }


}


