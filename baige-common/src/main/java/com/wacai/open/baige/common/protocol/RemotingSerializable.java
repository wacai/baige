package com.wacai.open.baige.common.protocol;

import com.alibaba.fastjson.JSON;
import java.nio.charset.Charset;

public abstract  class RemotingSerializable {

  public final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");


  public String toJson() {
    return toJson(false);
  }


  public String toJson(final boolean prettyFormat) {
    return toJson(this, prettyFormat);
  }

  public static String toJson(final Object obj, boolean prettyFormat) {
    return JSON.toJSONString(obj, prettyFormat);
  }


  public static <T> T fromJson(String json, Class<T> classOfT) {
    return JSON.parseObject(json, classOfT);
  }


  public byte[] encode() {
    final String json =  toJson();
    if (json != null) {
      return json.getBytes();
    }
    return null;
  }
  public static byte[] encode(final Object obj) {
    final String json = toJson(obj, false);
    if (json != null) {
      return json.getBytes(CHARSET_UTF8);
    }
    return null;
  }

  public static <T> T decode(final byte[] data, Class<T> classOfT) {
    final String json = new String(data, CHARSET_UTF8);
    return fromJson(json, classOfT);
  }


}
