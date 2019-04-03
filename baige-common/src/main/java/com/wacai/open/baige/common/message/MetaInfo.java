package com.wacai.open.baige.common.message;

import com.wacai.open.baige.common.protocol.RemotingSerializable;

/**
 * 仅用于server给client发送request command用。
 *
 */
public class MetaInfo extends RemotingSerializable {

  private long time = System.currentTimeMillis();


  public long getTime() {
    return time;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("MetaInfo[time:").append(this.time).append("]");
    return s.toString();
  }
}
