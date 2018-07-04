package com.wacai.open.baige.common.protocol.produce;

public class ProduceUtil {

  private static final String PRODUCER_GROUP_NAME_PREFIX = "baige_sdk_producer_group_";

  public static String getProducerGroup(String appKey) {
    return PRODUCER_GROUP_NAME_PREFIX + appKey;
  }



}
