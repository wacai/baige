package com.wacai.open.baige.remoting.protocol;

public enum  SerializeType {

  JSON((byte)0),
  BAIGE((byte)1);

  SerializeType(byte code) {
    this.code = code;
  }

  private byte code;


  public byte getCode() {
    return code;
  }

  public static SerializeType valueOf(byte code) {
    for (SerializeType serializeType : SerializeType.values()) {
      if (serializeType.getCode() == code) {
        return serializeType;
      }
    }
    return null;
  }

}
