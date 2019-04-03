package com.wacai.open.baige.common.util;

public class MiscUtil {
  public static long getPID() {
    String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
    if (processName != null && processName.length() > 0) {
      try {
        return Long.parseLong(processName.split("@")[0]);
      }
      catch (Exception e) {
        return 0;
      }
    }

    return 0;
  }




}
