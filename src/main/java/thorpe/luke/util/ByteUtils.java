package thorpe.luke.util;

import java.io.*;

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
}
