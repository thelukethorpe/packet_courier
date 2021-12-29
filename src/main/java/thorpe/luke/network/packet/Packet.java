package thorpe.luke.network.packet;

import java.util.Arrays;

public class Packet {
  private final byte[] data;

  public Packet(byte[] data) {
    this.data = data;
  }

  public static Packet of(String message) {
    return new Packet(message.getBytes());
  }

  public String asString() {
    return this.toString();
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Packet) {
      Packet that = (Packet) obj;
      return Arrays.equals(this.data, that.data);
    }
    return false;
  }

  @Override
  public String toString() {
    return new String(data);
  }
}
