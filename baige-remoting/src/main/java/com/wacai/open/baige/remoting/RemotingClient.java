package com.wacai.open.baige.remoting;

import com.wacai.open.baige.remoting.exception.RemotingConnectException;
import com.wacai.open.baige.remoting.exception.RemotingSendRequestException;
import com.wacai.open.baige.remoting.exception.RemotingTimeoutException;
import com.wacai.open.baige.remoting.exception.RemotingTooMuchRequestException;
import com.wacai.open.baige.remoting.listener.RemotingClientListener;
import com.wacai.open.baige.remoting.netty.NettyRequestProcessor;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface RemotingClient extends RemotingService {

  RemotingCommand invokeSync(final String addr, final RemotingCommand request,
      final long timeoutMills) throws InterruptedException, RemotingConnectException,
      RemotingSendRequestException, RemotingTimeoutException;

  void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMills, final InvokeCallback invokeCallback)
      throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
      RemotingTimeoutException, RemotingSendRequestException;


  void registerProcessor(final short requestCode, final NettyRequestProcessor processor,
      final ExecutorService executorService);


  void registerListener(RemotingClientListener remotingClientListener);

  RemotingClientListener getRemotingClientListener();

  Future<Boolean> reconnect(long connectTimeoutMs) throws Exception;

  void suspend();

  void resume();


  void sendKeepaliveHeartBeat(long sendTimeoutMs) throws Exception;


}
