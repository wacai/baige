package com.wacai.open.baige.remoting;

import com.wacai.open.baige.remoting.netty.ResponseFuture;

public interface InvokeCallback {

  void operationComplete(final ResponseFuture responseFuture);

}
