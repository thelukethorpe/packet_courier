package thorpe.luke.network.packet;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import thorpe.luke.util.ByteUtils;

public class Packet implements PacketWrapper<Packet> {
  private final byte[] data;

  public Packet(byte[] data) {
    this.data = data;
  }

  public static Packet of(Serializable serializable) {
    try {
      return new Packet(ByteUtils.serialize(serializable));
    } catch (IOException e) {
      throw new PacketException(e);
    }
  }

  public <T> Optional<T> tryParse() {
    try {
      return Optional.of((T) ByteUtils.deserialize(data));
    } catch (ClassCastException | ClassNotFoundException e) {
      return Optional.empty();
    } catch (IOException e) {
      throw new PacketException(e);
    }
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public Packet map(Function<Packet, Packet> function) {
    return function.apply(this);
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
