package thorpe.luke.network.packet;

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import thorpe.luke.util.ByteUtils;

public class Packet implements PacketWrapper<Packet> {
  private final List<Byte> data;

  public Packet(List<Byte> data) {
    this.data = Collections.unmodifiableList(data);
  }

  public static Packet fromBytes(byte... bytes) {
    return new Packet(ByteUtils.toList(bytes));
  }

  public static Packet of(Serializable serializable) {
    try {
      return new Packet(ByteUtils.toList(ByteUtils.serialize(serializable)));
    } catch (IOException e) {
      throw new PacketException(e);
    }
  }

  public <T> Optional<T> tryParse() {
    try {
      return Optional.ofNullable((T) ByteUtils.deserialize(ByteUtils.toArray(getData())));
    } catch (ClassCastException
        | ClassNotFoundException
        | StreamCorruptedException
        | EOFException e) {
      return Optional.empty();
    } catch (IOException e) {
      throw new PacketException(e);
    }
  }

  public <T> Optional<T> tryParse(Class<T> clazz) {
    return tryParse();
  }

  public List<Byte> getData() {
    return new ArrayList<>(data);
  }

  public int countBytes() {
    return data.size();
  }

  @Override
  public int hashCode() {
    return data.hashCode();
  }

  public Packet bytewiseMap(Function<Byte, Byte> function) {
    return new Packet(this.getData().stream().map(function).collect(Collectors.toList()));
  }

  @Override
  public Packet map(Function<Packet, Packet> function) {
    return function.apply(this);
  }

  @Override
  public Packet copy() {
    return new Packet(new ArrayList<>(data));
  }

  @Override
  public Packet getPacket() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Packet) {
      Packet that = (Packet) obj;
      return this.data.equals(that.data);
    }
    return false;
  }

  @Override
  public String toString() {
    return "Packet(length="
        + data.size()
        + ",contents=["
        + data.stream().map(Object::toString).collect(Collectors.joining(","))
        + "])";
  }
}
