package com.wacai.open.baige.common.protocol.authorize;

import com.wacai.open.baige.common.protocol.RemotingSerializable;

public class AuthorizeData extends RemotingSerializable {


  private String appkey;
  private String text;
  private String sign;



  public AuthorizeData(String appkey, String text, String sign) {
    this.appkey = appkey;
    this.text = text;
    this.sign = sign;
  }

  public String getAppkey() {
    return appkey;
  }

  public void setAppkey(String appkey) {
    this.appkey = appkey;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getSign() {
    return sign;
  }

  public void setSign(String sign) {
    this.sign = sign;
  }


  @Override
  public int hashCode() {
   String id = this.getID();
   return id == null ? 0 : id.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || !(object instanceof AuthorizeData)) {
      return false;
    }
    AuthorizeData authorizeData = (AuthorizeData)object;
    String id = authorizeData.getID();
    return id != null && id.equals(this.getID());
  }

  public String getID() {
    StringBuilder s = new StringBuilder();
    if (this.appkey != null) {
      s.append(this.appkey).append("@");
    }
    if (this.text != null) {
      s.append(this.text).append("@");
    }
    if (this.sign != null) {
      s.append(this.sign).append("@");
    }
    return s.toString();
  }


  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("AuthorizeData[");
    s.append("appkey=").append(this.appkey).append(",");
    s.append("text=").append(this.text).append(",");
    s.append("sign=").append(this.sign).append(",");
    s.append("]");
    return s.toString();
  }

}
