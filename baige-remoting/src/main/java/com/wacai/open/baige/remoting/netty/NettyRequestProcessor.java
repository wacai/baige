package com.wacai.open.baige.remoting.netty;

import com.wacai.open.baige.remoting.protocol.RemotingCommand;

public interface NettyRequestProcessor<T> {

//  RemotingCommand processRequest(ChannelHandlerContext channelHandlerContext,
//      RemotingCommand request) throws  Exception;


  /**
   * 处理request command ;
   * @param context 和request command处理相关的上下文
   * @param request request command .
   * @return  response command
   * @throws Exception  Exception
   */
    RemotingCommand processRequest(T context,
      RemotingCommand request) throws  Exception;

  /**
   * 异步处理请求。
   * @param context  异步请求上下文
   * @param request  请求Command
   * @param asyncProcessCallback  AsyncProcessCallback
   */
    void asyncProcessRequest(T context, RemotingCommand request, AsyncProcessCallback asyncProcessCallback);


    interface AsyncProcessCallback {
      void onCompleteProcess(RemotingCommand response);
      void onException(Throwable t);
    }







}
