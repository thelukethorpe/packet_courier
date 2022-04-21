package thorpe.luke.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UniqueLoopbackIpv4AddressGenerator {
  private static final int MINIMUM_LOOPBACK_VALUE = 3;
  private static final int MAXIMUM_LOOPBACK_VALUE = 1 << 24;
  private static final byte LOOPBACK_HEADER = 127;

  private int nextLoopbackValue = MINIMUM_LOOPBACK_VALUE;

  public synchronized InetAddress generateUniqueIpv4Address() throws UnknownHostException {
    if (nextLoopbackValue == MAXIMUM_LOOPBACK_VALUE) {
      return null;
    }

    byte[] ipv4Address = new byte[4];
    ipv4Address[0] = LOOPBACK_HEADER;
    ipv4Address[1] = (byte) (nextLoopbackValue >> 16);
    ipv4Address[2] = (byte) (nextLoopbackValue >> 8);
    ipv4Address[3] = (byte) nextLoopbackValue;

    nextLoopbackValue++;

    return Inet4Address.getByAddress(ipv4Address);
  }
}
