package com.wacai.open.baige.sdk;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MQClientInstanceManager {

  private static MQClientInstanceManager instance = new MQClientInstanceManager();

  private AtomicInteger indexGenerator = new AtomicInteger();

  private ConcurrentHashMap<String/*clientid*/, MQClientInstance>  clientId2MQClientInstances
       = new ConcurrentHashMap<>();

  private MQClientInstanceManager() {

  }


  public static MQClientInstanceManager getInstance() {
    return instance;
  }


  public MQClientInstance getMQClientInstance(final ClientConfig clientConfig) {
    String clientId = clientConfig.buildMQClientId();
    MQClientInstance mqClientInstance = this.clientId2MQClientInstances.get(clientId);
    if (mqClientInstance == null) {
      mqClientInstance = new MQClientInstance(clientConfig, indexGenerator.getAndIncrement(),
          clientId);
      MQClientInstance old = this.clientId2MQClientInstances.putIfAbsent(clientId, mqClientInstance);
      if (old != null) {
        mqClientInstance = old;
      }
    }
    return mqClientInstance;
  }

  public void removeMQClientInstance(final String clientId) {
    this.clientId2MQClientInstances.remove(clientId);
  }







}
