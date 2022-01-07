package thorpe.luke.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import org.junit.Test;

public class ByteUtilsTest {

  public static class TestObject implements Serializable {
    public final String string;
    private final int integer;
    private final double real;

    public TestObject(String string, int integer, double real) {
      this.string = string;
      this.integer = integer;
      this.real = real;
    }

    @Override
    public int hashCode() {
      return Objects.hash(string, integer, real);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof TestObject) {
        TestObject that = (TestObject) obj;
        return this.string.equals(that.string)
            && this.integer == that.integer
            && this.real == that.real;
      }
      return false;
    }
  }

  @Test
  public void testSerializeDeserializeCycle() {
    TestObject testObject = new TestObject("hello world!", 42, 3.14);
    byte[] serializedObject = new byte[0];
    try {
      serializedObject = ByteUtils.serialize(testObject);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Object serialization failed.");
    }
    Object deserializedObject = null;
    try {
      deserializedObject = ByteUtils.deserialize(serializedObject);
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
      fail("Object deserialization failed.");
    }
    assertThat(deserializedObject).isInstanceOf(TestObject.class);
    TestObject deserializedTestObject = (TestObject) deserializedObject;
    assertThat(deserializedTestObject).isEqualTo(testObject);
  }
}
