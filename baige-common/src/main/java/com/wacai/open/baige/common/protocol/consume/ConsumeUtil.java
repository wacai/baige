package com.wacai.open.baige.common.protocol.consume;

public class ConsumeUtil {

  private static final String CONSUME_GROUP_NAME_PREFIX = "baige_sdk_consumer_group_";

  public static String getConsumerGroupName(String appKey) {
    return CONSUME_GROUP_NAME_PREFIX + appKey;
  }



}
