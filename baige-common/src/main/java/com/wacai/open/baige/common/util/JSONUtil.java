package com.wacai.open.baige.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import java.nio.charset.Charset;
import java.util.List;

public class JSONUtil {

  private  final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");


  public static String toJson(final Object obj, boolean prettyFormat) {
    return JSON.toJSONString(obj, prettyFormat);
  }


  public static <T> T fromJson(String json, Class<T> classOfT) {
    return JSON.parseObject(json, classOfT);
  }


  public static  byte[] encode(final Object obj) {
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

  public static <T> List<T> decodeList(final byte[] data, Class<T> classOfT) {
    final String json = new String(data, CHARSET_UTF8);
    return JSONArray.parseArray(json, classOfT);
  }

}