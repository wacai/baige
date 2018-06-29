package com.wacai.open.baige.remoting.netty.websocket.listener;

import com.wacai.open.baige.remoting.netty.websocket.WsSocket;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import java.net.InetSocketAddress;
import org.eclipse.jetty.websocket.api.Session;

public  class SocketListenerAdaptor implements SocketListener  {

  @Override
  public void onWebSocketClose(WsSocket wsSocket, int statusCode, String reason,
      InetSocketAddress remoteAddr) {

  }

  @Override
  public void onWebSocketConnect(WsSocket wsSocket, Session session, int connectTimes) throws Exception {

  }

  @Override
  public void onWebSocketError(WsSocket wsSocket, Throwable error) {

  }

  @Override
  public void onRecvCommand(WsSocket wsSocket, RemotingCommand remotingCommand) {

  }
}
