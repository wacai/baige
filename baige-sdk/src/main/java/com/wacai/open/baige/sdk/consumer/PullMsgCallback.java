package com.wacai.open.baige.sdk.consumer;

public interface PullMsgCallback {

  void onSuccess(/*final PullRequest pullRequest,*/ final PullResult pullResult);

  void onException(/*final PullRequest pullRequest, */ final Throwable e);

  void onMsgNotFound(final PullRequest pullRequest);

}
