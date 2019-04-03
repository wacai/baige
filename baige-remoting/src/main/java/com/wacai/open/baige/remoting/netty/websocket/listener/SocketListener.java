package com.wacai.open.baige.remoting.netty.websocket.listener;


import com.wacai.open.baige.remoting.netty.websocket.WsSocket;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import java.net.InetSocketAddress;
import org.eclipse.jetty.websocket.api.Session;

/**
 * 接收socket的各种事件
 */
public interface SocketListener {
  void onWebSocketClose(WsSocket wsSocket, int statusCode, String reason,
      InetSocketAddress remoteAddr);

  void onWebSocketConnect(WsSocket wsSocket, Session session, int connectTimes) throws Exception;
  void onWebSocketError(WsSocket wsSocket, Throwable error);
  void onRecvCommand(WsSocket wsSocket, RemotingCommand remotingCommand);
}
