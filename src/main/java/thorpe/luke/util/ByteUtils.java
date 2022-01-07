package thorpe.luke.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ByteUtils {

  public static byte[] serialize(Serializable serializable) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(serializable);
    return byteArrayOutputStream.toByteArray();
  }

  public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
    return objectInputStream.readObject();
  }

  public static List<Byte> toList(byte[] array) {
    List<Byte> list = new ArrayList<>(array.length);
    for (byte b : array) {
      list.add(b);
    }
    return list;
  }

  public static byte[] toArray(List<Byte> list) {
    byte[] array = new byte[list.size()];
    int i = 0;
    for (Byte b : list) {
      array[i++] = b;
    }
    return array;
  }

  public static byte flip(byte b) {
    return (byte) (b ^ 0b11111111);
  }
}
