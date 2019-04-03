package com.wacai.open.baige.common.message;

import com.wacai.open.baige.common.protocol.RemotingSerializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息序列化格式如下：
 * 消息个数（2字节）| 消息1长度（4字节） | 消息1 | 消息2长度（4字节）| 消息2
 *
 * 每一个消息的格式如下 ：
 * mgs key 长度（2字节） | msg key|offset(8字节） | payload 长度（4字节） | payload |
 */
public class MessageCodec {

  public static List<Message> decode(byte[] msgsBytes) {
    if (msgsBytes == null) {
      return null;
    }
    ByteBuffer byteBuffer =  ByteBuffer.wrap(msgsBytes);
    short msgCount = byteBuffer.getShort();

    if (msgCount <= 0) {
      return null;
    }
    List<Message> messages = new ArrayList<>(msgCount);
    int msgLength = 0;
    for (short i = 0; i < msgCount; ++i) {
      msgLength = byteBuffer.getInt();
      byte[] msgBytes = new byte[msgLength];
      byteBuffer.get(msgBytes);
      Message message = deserialize(msgBytes);
      messages.add(message);
    }
    return messages;

  }

  public static byte[] encode(List<Message> messages) {

    int messageBytesLength = getMessagesByteSize(messages);
    if (messageBytesLength <= 0) {
      return null;
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(messageBytesLength);
    byteBuffer.putShort((short) messages.size()); //消息个数
    for (Message message : messages) {
      byteBuffer.putInt(getMessageByteSize(message)); //消息长度。
      if (message.getMsgKey() != null) { // msg key length & msg key
        byteBuffer.putShort((short)message.getMsgKey().length());
        byteBuffer.put(message.getMsgKey().getBytes());
      } else {
        byteBuffer.putShort((short)0);
      }
      byteBuffer.putLong(message.getOffset());  //offset
      if (message.getPayload() != null ) { // payload length & payload
        byteBuffer.putInt(message.getPayload().length);
        byteBuffer.put(message.getPayload());
      } else {
        byteBuffer.putInt(0);
      }
    }

    return byteBuffer.array();
  }

  private static int getMessageByteSize(Message message) {
    if (message == null) {
      return 0;
    }
    int size = 0;
    size += 2; // msg key长度
    if (message.getMsgKey() != null) { // msg key
      size += ((short)message.getMsgKey().length());
    }
    size += 8; //offset;
    size += 4; //payload length;
    if (message.getPayload() != null) { // payload
      size += message.getPayload().length;
    }
    return size;
  }

  private static int getMessagesByteSize(List<Message> messages) {
    if (messages == null || messages.size() <= 0) {
      return 0;
    }

    int size = 0;
    size += 2; //消息个数
    for (Message message : messages) {
      size += 4; //消息长度
      size += getMessageByteSize(message);
    }
    return size;
  }

  private static Message deserialize(byte []msgBytes) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(msgBytes);

    Message message = new Message();
    short msgkeySize = byteBuffer.getShort();
    if (msgkeySize > 0) {
      byte []msgKeyBytes = new byte[msgkeySize];
      byteBuffer.get(msgKeyBytes);
      message.setMsgKey(new String(msgKeyBytes, RemotingSerializable.CHARSET_UTF8));
    }
    message.setOffset(byteBuffer.getLong());
    int payloadSize = byteBuffer.getInt();
    if (payloadSize > 0) {
      byte []payloadBytes = new byte[payloadSize];
      byteBuffer.get(payloadBytes);
      message.setPayload(payloadBytes);
    }

    return  message;


  }
//  public static void main(String []args) {
//    List<Message> messages = new ArrayList<>(2);
//    Message msg1 = new Message();
//    msg1.setPayload("payload".getBytes());
//    msg1.setOffset(0);
//    msg1.setMsgKey("msgkey1");
//    messages.add(msg1);
//
//    Message msg2 = new Message();
//    msg2.setPayload("payload2".getBytes());
//    msg2.setOffset(1);
//    msg2.setMsgKey("msgkey2");
//    messages.add(msg2);
//
//
//
//    byte []bytes = MessageCodec.encode(messages);
//
//    List<Message> msgs = MessageCodec.decode(bytes);
//    for(Message msg : msgs) {
//      System.out.println(msg);
//    }
//
//  }
}
