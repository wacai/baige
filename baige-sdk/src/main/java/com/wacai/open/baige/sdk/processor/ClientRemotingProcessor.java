package com.wacai.open.baige.sdk.processor;

import com.wacai.open.baige.common.message.Message;
import com.wacai.open.baige.common.message.MessageCodec;
import com.wacai.open.baige.common.message.MetaInfo;
import com.wacai.open.baige.common.protocol.RequestCode;
import com.wacai.open.baige.common.protocol.ResponseCode;
import com.wacai.open.baige.common.protocol.header.GetMetaInfoResponseHeader;
import com.wacai.open.baige.common.protocol.header.ResumePullMsgRequestHeader;
import com.wacai.open.baige.common.protocol.header.ResumePullMsgResponseHeader;
import com.wacai.open.baige.remoting.exception.RemotingCommandException;
import com.wacai.open.baige.remoting.netty.NettyRequestProcessor;
import com.wacai.open.baige.remoting.netty.websocket.WsSocket;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import com.wacai.open.baige.sdk.MQClientInstance;
import java.util.List;

public class ClientRemotingProcessor implements NettyRequestProcessor<WsSocket> {

  private final MQClientInstance mqClientInstance;

  public ClientRemotingProcessor(MQClientInstance mqClientInstance) {
    this.mqClientInstance = mqClientInstance;
  }


  @Override
  public RemotingCommand processRequest(WsSocket wsSocket,
      RemotingCommand request) throws Exception {
    switch(request.getCode()) {
      case RequestCode.GET_CLIENT_META_INFO:
        return this.getMetaInfo(wsSocket, request);
      case RequestCode.RESUME_PULL_MSG:
        return this.resumePullMsg(request);
      default:
        break;
    }
    return null;
  }

  @Override
  public void asyncProcessRequest(WsSocket context, RemotingCommand request,
      AsyncProcessCallback asyncProcessCallback) {

  }

  private RemotingCommand resumePullMsg(RemotingCommand request) throws RemotingCommandException {

    ResumePullMsgRequestHeader resumePullMsgRequestHeader = (ResumePullMsgRequestHeader)
        request.decodeCommandCustomHeader(ResumePullMsgRequestHeader.class);

    List<Message> messages = null;
    byte[] messageBytes =  request.getBody();
    if (messageBytes != null) {
        messages = MessageCodec.decode(messageBytes);
    }

    String consumerGroup = resumePullMsgRequestHeader.getConsumerGroup();
    String topic = resumePullMsgRequestHeader.getTopic();

    boolean resumePullMsgSuccess = mqClientInstance.getMqConsumerInner().resumePullMsg(consumerGroup, topic, messages);
    RemotingCommand responseCommand = RemotingCommand.createResponseCommand(ResumePullMsgResponseHeader.class);
    ResumePullMsgResponseHeader resumePullMsgResponseHeader = (ResumePullMsgResponseHeader) responseCommand.readCustomHeader();
    resumePullMsgResponseHeader.setResumePullMsgSuccess(resumePullMsgSuccess);

    return responseCommand;




  }


  private RemotingCommand getMetaInfo(WsSocket wsSocket, RemotingCommand request) {
    RemotingCommand response = RemotingCommand
        .createResponseCommand(GetMetaInfoResponseHeader.class);
    response.setBody(new MetaInfo().encode());
    response.setCode(ResponseCode.SUCCESS);
    return response;
  }
}
