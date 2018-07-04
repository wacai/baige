package com.wacai.open.baige.remoting.protocol;

import com.alibaba.fastjson.annotation.JSONField;
import com.wacai.open.baige.common.protocol.RemotingSysResponseCode;
import com.wacai.open.baige.common.protocol.header.CommandCustomHeader;
import com.wacai.open.baige.remoting.annotation.CFNotNull;
import com.wacai.open.baige.remoting.exception.RemotingCommandException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * */
public class RemotingCommand {

  public static final String SERIALIZE_TYPE_PROPERTY = "baige.serialize.type";
  public static final String SERIALIZE_TYPE_ENV = "BAIGE_SERIALIZE_TYPE";
  public static String RemotingVersionKey = "com.wacai.open.baige.remoting.version";

  private static AtomicInteger RequestId = new AtomicInteger(0);
  private static final String StringCanonicalName = String.class.getCanonicalName();//

  private static final String DoubleCanonicalName1 = Double.class.getCanonicalName();//
  private static final String DoubleCanonicalName2 = double.class.getCanonicalName();//

  private static final String IntegerCanonicalName1 = Integer.class.getCanonicalName();//
  private static final String IntegerCanonicalName2 = int.class.getCanonicalName();//

  private static final String LongCanonicalName1 = Long.class.getCanonicalName();//
  private static final String LongCanonicalName2 = long.class.getCanonicalName();//

  private static final String BooleanCanonicalName1 = Boolean.class.getCanonicalName();//
  private static final String BooleanCanonicalName2 = boolean.class.getCanonicalName();//




  /**
   * 0 REQUEST_COMMAND
   * 1 RESPONSE_COMMAND
   */
  private static final int RPC_TYPE = 0;

  /* 0 RPC
  *  1  One way  */
  private static final int RPC_ONEWAY = 1;


  private static volatile int ConfigVersion = -1;



  /** 缓存CommandCustomHeader子类的字段列表。
   * */
  private static final Map<Class<? extends CommandCustomHeader>, Field[]> classFieldsCache
      = new HashMap<>();

  /** 缓存字段和Annotation的映射关系 */
  private static final Map<Field, Annotation> notNullAnnotationCache = new HashMap<Field, Annotation>();

  private static final Map<Class, String> canonicalNameCache = new HashMap<Class, String>();






//  public static byte[] markProtocolType(int source, SerializeType type) {
//    //比如： source 是0xabcdef13
//    byte[] result = new byte[4];
//    result[0] = type.getCode(); // 高字节写的是SerializeType
//    result[1] = (byte) ((source >> 16) & 0xFF); // cd
//    result[2] = (byte) ((source >> 8) & 0xFF); //ef
//    result[3] = (byte) (source & 0xFF); //13
//    return result;
//  }


  /*header data   开始*/
  /* 00000000
      从右到左
      第一个bit: 请求应答标记位 0 Request 1 Response
      第二个bit: RPC标记位     0  RPC    1 One Way;
  *  */
  private short flag = 0;

  /*如果 RemotingCommand 是请求，则标志操作码，接收方根据操作码进行操作； 如果是响应，则0标识成功，其他标识各种错误代码。*/
  private short code;

  /*2017-04-17 添加， 此字段用于同步调用响应时设置和 code字段一样的值，方便sdk区分响应， 类似于http status code*/
  private short respCode;

  /*请求发起方在同一连接上的不同请求标志代码，多线程连接复用使用；应答方不做修改，直接返回*/
  private int opaque;


  /*topic*/
  private String topic;


  /*如果是request ，则传输自定义文本；如果是响应，则传输错误详情*/
  private String remark;

  /*请求或者应答的自定义字段*/
  private HashMap<String, String> extFields;

  /*header data  结束*/

  private transient byte[] body;


  /**提供给业务自定义扩展header data 中的{@link RemotingCommand#extFields}*/
  private transient CommandCustomHeader customHeader;

  public RemotingCommand() {
    this.opaque =  RequestId.getAndIncrement();
  }

  public RemotingCommand(boolean createOpaque) {
    if (createOpaque) {
      this.opaque =  RequestId.getAndIncrement();
    }
  }


  public ByteBuffer encode() {
    /*header length : 2字节*/
    int length = 2;
    byte []headerData = this.headerEncode();
    /*加上header data length */
    length += headerData.length;
    /*加上body length*/
    if (body != null) {
      length += body.length;
    }
    /*头4字节是packet的总长度， length = header length（2字节）  + header长度 + body 长度*/
    ByteBuffer result = ByteBuffer.allocate(4 + length);
    //header length（2字节）  + header长度 + body 长度
    result.putInt(length);
    //header length
    result.putShort((short) headerData.length);
    // header data
    result.put(headerData);
    // body data
    if (this.body != null) {
      result.put(this.body);
    }
    //switch to read mode ;
    result.flip();
    return result;
  }

  public ByteBuffer encodeHeader() {
    return encodeHeader(this.body != null ? this.body.length : 0);
  }

  /*getter and setter methods*/

  public short getCode() {
    return code;
  }

  public void setCode(short code) {
    this.code = code;
  }


  public short getRespCode() {
    return respCode;
  }

  public void setRespCode(short respCode) {
    this.respCode = respCode;
  }

  public int getOpaque() {
    return opaque;
  }


  public void setOpaque(int opaque) {
    this.opaque = opaque;
  }


  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }


  public short getFlag() {
    return flag;
  }

  public void setFlag(short flag) {
    this.flag = flag;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public HashMap<String, String> getExtFields() {
    return extFields;
  }

  public void setExtFields(HashMap<String, String> extFields) {
    this.extFields = extFields;
  }


  public byte[] getBody() {
    return body;
  }

  public void setBody(byte[] body) {
    this.body = body;
  }

  public CommandCustomHeader getCustomHeader() {
    return customHeader;
  }

  public void setCustomHeader(CommandCustomHeader customHeader) {
    this.customHeader = customHeader;
  }


  /*getter and setter methods*/


  public static Object decode(ByteBuffer byteBuffer) {
    /*从外部送进来的byteBuffer是
     header length(2) | header data | body data
    * */
    int length = byteBuffer.limit(); // 2 + header data 长度 + body data 长度
    short headerLength = byteBuffer.getShort(); //头2字节header length
    byte []headerData = new byte[headerLength];
    byteBuffer.get(headerData);//读入headerData;

    RemotingCommand cmd = headerDecode(headerData);
    int bodyLength = length - 2 - headerLength;
    byte[] bodyData = null;
    if (bodyLength > 0) {
      bodyData = new byte[bodyLength];
      byteBuffer.get(bodyData);
    }

    cmd.body = bodyData;
    return cmd;

  }





  @JSONField(serialize = false)
  public RemotingCommandType getType() {
    if (this.isResponseType()) {
      return RemotingCommandType.RESPONSE_COMMAND;
    } else {
      return RemotingCommandType.REQUEST_COMMAND;
    }
  }

  @JSONField(serialize = false)
  public boolean isResponseType() {
    int bits = 1 << RPC_TYPE;
    return (this.flag & bits) == bits;
  }



  @JSONField(serialize = false)
  public boolean isOnewayRPC() {
    int bits = 1 << RPC_ONEWAY;
    return (this.flag & bits) == bits;
  }


  public void markResponseType() {
    int bits = 1 << RPC_TYPE;
    this.flag |= bits;
  }

  public static RemotingCommand createRequestCommand(byte code, CommandCustomHeader customHeader, String topic) {
    RemotingCommand cmd = new RemotingCommand();
    cmd.setCode(code);
    cmd.setTopic(topic);
    cmd.customHeader = customHeader;
    return cmd;
  }




  public static RemotingCommand createRequestCommand(byte code, CommandCustomHeader customHeader) {
    RemotingCommand cmd = new RemotingCommand();
    cmd.setCode(code);
    cmd.customHeader = customHeader;
    return cmd;
  }


  public static RemotingCommand createResponseCommand(Class<? extends CommandCustomHeader> classHeader) {
    RemotingCommand cmd = createResponseCommand(RemotingSysResponseCode.SYSTEM_ERROR,
        "not set any response code ", classHeader);
    return cmd;
  }

  public static RemotingCommand createResponseCommand(byte code, String remark) {
    return createResponseCommand(code, remark, null);

  }

  public static RemotingCommand createResponseCommand(byte code, String remark,
      Class<? extends CommandCustomHeader> clazzHeader ) {
    RemotingCommand cmd = new RemotingCommand();
    cmd.markResponseType();
    cmd.setCode(code);
    cmd.setRemark(remark);
    if (clazzHeader != null) {
      try {
        CommandCustomHeader customHeader = clazzHeader.newInstance();
        cmd.setCustomHeader(customHeader);
      } catch (InstantiationException e) {
        return null;
      } catch (IllegalAccessException e) {
        return null;
      }
    }
    return cmd;
  }


  /**
   * @param bodyLength
   * @return
   */
  private ByteBuffer encodeHeader(final int bodyLength) {
    // header length size;
    int length = 2;

    //header data length
    byte []headerData;
    headerData = this.headerEncode();
    length += headerData.length;

    /*body data length*/
    length += bodyLength;

    //packet去掉了body以后的字节数组。
    ByteBuffer result = ByteBuffer.allocate(4 + length - bodyLength);
    //length
    result.putInt(length);
    //header length
    result.putShort((short) headerData.length);
    //header data
    result.put(headerData);
    //switch to read mode ;
    result.flip();

    return result;

  }


  /**
   * 把RemotingCommand类中的extFields属性 解析成CommandCustomHeader 子类的属性。
   * @param classHeader
   * @return
   */
  public CommandCustomHeader decodeCommandCustomHeader(Class<? extends CommandCustomHeader> classHeader)
      throws RemotingCommandException {
    CommandCustomHeader objectHeader;
    try {
      objectHeader = classHeader.newInstance();
    }
    catch (InstantiationException e) {
      return null;
    }
    catch (IllegalAccessException e) {
      return null;
    }

    if (this.extFields != null) {
      Field[] fields = getClassFields(classHeader);
      for (Field field : fields) {
        if (!Modifier.isStatic(field.getModifiers())) {
          String fieldName = field.getName();
          if (!fieldName.startsWith("this")) {
            try {
              String value = this.extFields.get(fieldName);
              if (null == value) {
                /**如果标注了{@link CFNotNull } 标注，并且没有获取到值，则抛出异常*/
//                Annotation annotation = getNotNullAnnotation(field);
//                if (annotation != null) {
//                  throw new RemotingCommandException("the custom field <" + fieldName + "> is null");
//                }
                continue;
              }

              field.setAccessible(true);
              String type = getCanonicalName(field.getType());
              Object valueParsed;

              if (type.equals(StringCanonicalName)) {
                valueParsed = value;
              }
              else if (type.equals(IntegerCanonicalName1) || type.equals(IntegerCanonicalName2)) {
                valueParsed = Integer.parseInt(value);
              }
              else if (type.equals(LongCanonicalName1) || type.equals(LongCanonicalName2)) {
                valueParsed = Long.parseLong(value);
              }
              else if (type.equals(BooleanCanonicalName1) || type.equals(BooleanCanonicalName2)) {
                valueParsed = Boolean.parseBoolean(value);
              }
              else if (type.equals(DoubleCanonicalName1) || type.equals(DoubleCanonicalName2)) {
                valueParsed = Double.parseDouble(value);
              }
              else {
                throw new RemotingCommandException("the custom field <" + fieldName + "> type is not supported");
              }

              field.set(objectHeader, valueParsed);

            }
            catch (Throwable e) {
            }
          }
        }
      }

      objectHeader.checkFields();
    }

    return objectHeader;
  }

  public CommandCustomHeader readCustomHeader() {
    return customHeader;
  }


  @Override
  public String toString() {
    return "RemotingCommand [code=" + code + ", flag(B)="
        + Integer.toBinaryString(flag) + ", remark=" + remark + ", extFields=" + extFields  + "]";
  }

  private byte[] headerEncode() {
    this.makeCustomHeaderToNet();
    return Serializer.encodeHeader(this);

  }

  private static RemotingCommand headerDecode(byte []headerData) {

    RemotingCommand cmd = Serializer.decodeHeader(headerData);
    return cmd;
  }

  /**
   * 通过反射调用获取到{@link CommandCustomHeader} 子类的属性，写入{@link RemotingCommand#extFields}
   */
  private void makeCustomHeaderToNet() {
    if (this.customHeader == null) {
      return;
    }

    Field []fields = getClassFields(customHeader.getClass());
    if (null == this.extFields) {
      this.extFields = new HashMap<>();
    }
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        String name = field.getName();
        if (!name.startsWith("this")) {
          Object value = null;
          try {
            field.setAccessible(true);
            value = field.get(this.customHeader);
          } catch (IllegalArgumentException e) {

          } catch (IllegalAccessException e) {

          }
          if (value != null) {
            this.extFields.put(name, value.toString());
          }
        }
      }
    }
  }



  private Field[] getClassFields(Class <? extends CommandCustomHeader> classHeader) {
    Field[] fields =  classFieldsCache.get(classHeader);
    if (fields == null) {
      fields = classHeader.getDeclaredFields();
      synchronized (classFieldsCache) {
        classFieldsCache.put(classHeader, fields);
      }
    }
    return fields;
  }

  private static boolean isBlank(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if ((Character.isWhitespace(str.charAt(i)) == false)) {
        return false;
      }
    }
    return true;
  }


//  private Annotation getNotNullAnnotation(Field field) {
//    Annotation annotation = notNullAnnotationCache.get(field);
//
//    if (annotation == null) {
//      annotation = field.getAnnotation(CFNotNull.class);
//      synchronized (notNullAnnotationCache) {
//        notNullAnnotationCache.put(field, annotation);
//      }
//    }
//    return annotation;
//  }

  private String getCanonicalName(Class clazz) {
    String name = canonicalNameCache.get(clazz);

    if (name == null) {
      name = clazz.getCanonicalName();
      synchronized (canonicalNameCache) {
        canonicalNameCache.put(clazz, name);
      }
    }
    return name;
  }




}
