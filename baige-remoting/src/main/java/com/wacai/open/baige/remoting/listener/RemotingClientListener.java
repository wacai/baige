package com.wacai.open.baige.remoting.listener;

import java.net.InetSocketAddress;

public interface RemotingClientListener {


  void onConnect(String serverAddr, int connectTimes);

  void onClose(InetSocketAddress remoteAddr);

  void onRemotingError(Throwable t); 

}
