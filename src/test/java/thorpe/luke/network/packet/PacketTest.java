package thorpe.luke.network.packet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Test;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.worker.WorkerAddress;
import thorpe.luke.util.ByteUtils;

public class PacketTest {

  public Packet flipBits(Packet packet) {
    return packet.bytewiseMap(ByteUtils::flip);
  }

  @Test
  public void testPacketStringConversion() {
    String message = "hello there!";
    Packet messageAsPacket = Packet.of(message);
    assertThat(messageAsPacket.tryParse()).hasValue(message);
  }

  @Test
  public void testPacketAddressConversion() {
    NodeAddress nodeAddress = new NodeAddress("node");
    WorkerAddress workerAddress = new WorkerAddress("worker", nodeAddress);
    Packet workerAddressAsPacket = Packet.of(workerAddress);
    assertThat(workerAddressAsPacket.tryParse()).hasValue(workerAddress);
  }

  @Test
  public void testBadParseReturnsEmptyOptional() {
    String message = "hello there!";
    Packet messageAsPacket = Packet.of(message);
    Packet corruptedPacket = flipBits(messageAsPacket);
    assertThat(corruptedPacket.tryParse()).isEmpty();
  }

  @Test
  public void testCorruptedPacketStringStillParses() {
    String message = "hello there!";
    Packet packet = Packet.of(message);
    List<Byte> data = packet.getData();
    data.set(11, (byte) (data.get(11) ^ 0b00000010));
    Packet corruptedPacket = new Packet(data);
    assertThat(corruptedPacket.tryParse(String.class)).hasValue("hellm there!");
  }

  @Test
  public void testPacketEquals() {
    byte byte0 = 0b00000000;
    byte byte1 = 0b00000001;
    byte byte2 = 0b00000010;
    byte byte3 = 0b00000011;

    assertThat(Packet.fromBytes(byte0, byte1, byte2, byte3))
        .isEqualTo(Packet.fromBytes(byte0, byte1, byte2, byte3));
    assertThat(Packet.fromBytes(byte0, byte1, byte2))
        .isNotEqualTo(Packet.fromBytes(byte0, byte1, byte2, byte3));

    assertThat(Packet.fromBytes(byte1, byte3)).isEqualTo(Packet.fromBytes(byte1, byte3));
    assertThat(Packet.fromBytes(byte1, byte2)).isNotEqualTo(Packet.fromBytes(byte1, byte3));
  }

  @Test
  public void testPacketToString() {
    byte byte0 = 0b00000000;
    byte byte1 = 0b00000001;
    byte byte2 = 0b00000010;
    byte byte3 = 0b00000011;

    assertThat(Packet.fromBytes(byte0)).asString().isEqualTo("Packet(length=1,contents=[0])");
    assertThat(Packet.fromBytes(byte2, byte1, byte3, byte0))
        .asString()
        .isEqualTo("Packet(length=4,contents=[2,1,3,0])");
  }
}
