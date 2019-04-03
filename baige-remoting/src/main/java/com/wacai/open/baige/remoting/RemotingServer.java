package com.wacai.open.baige.remoting;

import com.wacai.open.baige.remoting.exception.RemotingSendRequestException;
import com.wacai.open.baige.remoting.exception.RemotingTimeoutException;
import com.wacai.open.baige.remoting.exception.RemotingTooMuchRequestException;
import com.wacai.open.baige.remoting.netty.NettyRequestProcessor;
import com.wacai.open.baige.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
import java.util.concurrent.ExecutorService;

public interface RemotingServer extends RemotingService {


  void registerProcessor(final short requestCode, final NettyRequestProcessor processor,
      final ExecutorService executorService);


  RemotingCommand invokeSync(final Channel channel, final RemotingCommand request,
      final long timeoutMillis) throws InterruptedException, RemotingSendRequestException,
      RemotingTimeoutException;


  void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis,
      final InvokeCallback invokeCallback) throws InterruptedException,
      RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;



}
