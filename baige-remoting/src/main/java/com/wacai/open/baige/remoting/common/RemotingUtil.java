package com.wacai.open.baige.remoting.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotingUtil {


  private static final Logger LOGGER = LoggerFactory.getLogger(RemotingUtil.class);

  public static final String OS_NAME = System.getProperty("os.name");

  private static boolean isLinuxPlatform = false;
  private static boolean isWindowsPlatform = false;


  static {
    if (OS_NAME != null && OS_NAME.toLowerCase().indexOf("linux") >= 0) {
      isLinuxPlatform = true;
    }

    if (OS_NAME != null && OS_NAME.toLowerCase().indexOf("windows") >= 0) {
      isWindowsPlatform = true;
    }
  }


  public static String getLocalAddress() {
    try {
      // Traversal Network interface to get the first non-loopback and non-private address
      Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
      ArrayList<String> ipv4Result = new ArrayList<String>();
      ArrayList<String> ipv6Result = new ArrayList<String>();
      while (enumeration.hasMoreElements()) {
        final NetworkInterface networkInterface = enumeration.nextElement();
        final Enumeration<InetAddress> en = networkInterface.getInetAddresses();
        while (en.hasMoreElements()) {
          final InetAddress address = en.nextElement();
          if (!address.isLoopbackAddress()) {
            if (address instanceof Inet6Address) {
              ipv6Result.add(normalizeHostAddress(address));
            }
            else {
              ipv4Result.add(normalizeHostAddress(address));
            }
          }
        }
      }

      // prefer ipv4
      if (!ipv4Result.isEmpty()) {
        for (String ip : ipv4Result) {
          if (ip.startsWith("127.0") || ip.startsWith("192.168")) {
            continue;
          }

          return ip;
        }

        return ipv4Result.get(ipv4Result.size() - 1);
      }else if (!ipv6Result.isEmpty()) {
        return ipv6Result.get(0);
      }
      //If failed to find,fall back to localhost
      final InetAddress localHost = InetAddress.getLocalHost();
      return normalizeHostAddress(localHost);
    }
    catch (SocketException e) {
      LOGGER.warn("getLocalAddress catch SocketException", e);
    }
    catch (UnknownHostException e) {
      LOGGER.warn("getLocalAddress catch UnknownHostException", e);
    }

    return null;
  }

  public static String normalizeHostAddress(final InetAddress localHost) {
    if (localHost instanceof Inet6Address) {
      return "[" + localHost.getHostAddress() + "]";
    }
    else {
      return localHost.getHostAddress();
    }
  }

  public static String parseChannelRemoteAddr(final Channel channel) {
    if (null == channel) {
      return "";
    }
    final SocketAddress remote = channel.remoteAddress();
    final String addr = remote != null ? remote.toString() : "";

    if (addr.length() > 0) {
      int index = addr.lastIndexOf("/");
      if (index >= 0) {
        return addr.substring(index + 1);
      }

      return addr;
    }

    return "";
  }


  public static void closeChannel(Channel channel) {
    final String addrRemote = parseChannelRemoteAddr(channel);
    channel.close().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        LOGGER.info("closeChannel: close the connection to remote address[{}] result: {}",
            addrRemote,
            future.isSuccess());
      }
    });
  }


  public static String exceptionSimpleDesc(final Throwable e) {
    StringBuffer sb = new StringBuffer();
    if (e != null) {
      sb.append(e.toString());

      StackTraceElement[] stackTrace = e.getStackTrace();
      if (stackTrace != null && stackTrace.length > 0) {
        StackTraceElement elment = stackTrace[0];
        sb.append(", ");
        sb.append(elment.toString());
      }
    }

    return sb.toString();
  }


  public static SocketAddress string2SocketAddress(final String addr) {
    String[] s = addr.split(":");
    InetSocketAddress isa = new InetSocketAddress(s[0], Integer.valueOf(s[1]));
    return isa;
  }


  public static boolean isLinuxPlatform() {
    return isLinuxPlatform;
  }


  public static boolean isWindowsPlatform() {
    return isWindowsPlatform;
  }





}
