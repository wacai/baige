package com.wacai.open.baige.remoting.protocol;

import com.wacai.open.baige.common.protocol.RemotingSerializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 自主实现的Serializer
 */
public class Serializer {


  public static byte[] encodeHeader(RemotingCommand cmd) {

    byte []topicBytes = null;
    short topicLength = 0;
    if (cmd.getTopic() != null && cmd.getTopic().length() > 0) {
      topicBytes = cmd.getTopic().getBytes(RemotingSerializable.CHARSET_UTF8);
      topicLength = (short) topicBytes.length;
    }

    byte []remarkBytes = null;
    short  remarkLen = 0;
    if (cmd.getRemark() != null && cmd.getRemark().length() > 0) {
      remarkBytes = cmd.getRemark().getBytes(RemotingSerializable.CHARSET_UTF8);
      remarkLen = (short) remarkBytes.length;
    }

    byte []extFieldsBytes = null;
    short extLen = 0;
    if (cmd.getExtFields() != null && !cmd.getExtFields().isEmpty()) {
      extFieldsBytes = mapSerialize(cmd.getExtFields());
      extLen = (short) extFieldsBytes.length;
    }



    /**计算总字节数, {@link RemotingCommand} */
    int totalLength = getHeaderLength(topicLength, remarkLen, extLen);
    ByteBuffer headerBuffer = ByteBuffer.allocate(totalLength);
    // flag
    headerBuffer.putShort(cmd.getFlag());
    // code
    headerBuffer.putShort(cmd.getCode());
    // 是响应则填入respCode, 是请求则填0
    if (cmd.isResponseType()) {
      headerBuffer.putShort(cmd.getRespCode());
    } else {
      headerBuffer.putShort((short)0);
    }
    //int opaque
    headerBuffer.putInt(cmd.getOpaque());
    //topic
    if (topicBytes != null) {
      headerBuffer.putShort(topicLength);
      headerBuffer.put(topicBytes);
    } else {
      headerBuffer.putShort((short)0);
    }
    //String remark
    if (remarkBytes != null) {
      headerBuffer.putShort(remarkLen);
      headerBuffer.put(remarkBytes);
    } else {
      headerBuffer.putShort((short)0);
    }

    //HashMap<String, String> extFields;
    if (extFieldsBytes != null) {
      headerBuffer.putShort(extLen);
      headerBuffer.put(extFieldsBytes);
    } else {
      headerBuffer.putShort((short) 0);
    }


    return headerBuffer.array();


  }


  private static HashMap<String,String> mapDeserialize(byte[] bytes) {

    if (bytes == null || bytes.length <= 0) {
      return null;
    }

    HashMap<String, String> map = new HashMap<>();

    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    short keySize = 0;
    byte []keyContent = null;
    short valSize = 0;
    byte []valContent = null;
    while (byteBuffer.hasRemaining()) {
      keySize = byteBuffer.getShort();
      keyContent = new byte[keySize];
      byteBuffer.get(keyContent);

      valSize = byteBuffer.getShort();
      valContent = new byte[valSize];
      byteBuffer.get(valContent);

      map.put(new String(keyContent, RemotingSerializable.CHARSET_UTF8),
          new String(valContent, RemotingSerializable.CHARSET_UTF8));
    }

    return map;

  }

  private static byte[] mapSerialize(HashMap<String, String> map) {

    // keySize(1字节) + key + valSize(2字节）+val

    if (null == map || map.isEmpty()) {
      return null;
    }

    int kvLength;
    short totalLength = 0;
    Iterator<Entry<String, String>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, String> entry = it.next();
      if (entry.getKey() != null && entry.getValue() != null) {
        kvLength = 2 + (short)(entry.getKey().getBytes(RemotingSerializable.CHARSET_UTF8).length)
             + 2 + (short)(entry.getValue().getBytes(RemotingSerializable.CHARSET_UTF8).length);
        totalLength += kvLength;
      }
    }
    ByteBuffer content = ByteBuffer.allocate(totalLength);
    byte []key;
    byte []val;
    it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> entry = it.next();
      if (entry.getKey() != null && entry.getValue() != null) {
        key = entry.getKey().getBytes(RemotingSerializable.CHARSET_UTF8);
        val = entry.getValue().getBytes(RemotingSerializable.CHARSET_UTF8);

        content.putShort((short) key.length);
        content.put(key);

        content.putShort((short) val.length);
        content.put(val);
      }
    }
    return content.array();
  }


  public static RemotingCommand decodeHeader(final byte []headerArray) {
    RemotingCommand cmd = new RemotingCommand();
    ByteBuffer headerBuffer = ByteBuffer.wrap(headerArray);
    cmd.setFlag(headerBuffer.getShort());
    cmd.setCode(headerBuffer.getShort());
    cmd.setRespCode(headerBuffer.getShort()); //2017-04-17 增加一个response code ，用于客户端同步调用区分响应。
    cmd.setOpaque(headerBuffer.getInt());

    short topicLength = headerBuffer.getShort();
    if (topicLength > 0) {
      byte []topicBytes = new byte[topicLength];
      headerBuffer.get(topicBytes);
      cmd.setTopic(new String(topicBytes, RemotingSerializable.CHARSET_UTF8));
    }

    short remarkLength = headerBuffer.getShort();
    if (remarkLength > 0) {
      byte []remark = new byte[remarkLength];
      headerBuffer.get(remark);
      cmd.setRemark(new String(remark, RemotingSerializable.CHARSET_UTF8));
    }

    int extFieldsLength = headerBuffer.getShort();
    if (extFieldsLength > 0) {
      byte[] extFieldsBytes = new byte[extFieldsLength];
      headerBuffer.get(extFieldsBytes);
      cmd.setExtFields(mapDeserialize(extFieldsBytes));
    }


    return cmd;


  }



  private static int getHeaderLength(short topicLength, short  remarkLength, int extFieldsLength) {
    int length = 2 // flag
      + 2 // code
      + 2 //resp_code
      + 4 // opaque
      + 2 // topic length
      + topicLength
      + 2 // remark length
      + remarkLength
      + 2 // ext_fields length
      + extFieldsLength;

    return length;
  }


}
