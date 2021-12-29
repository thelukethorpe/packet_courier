package thorpe.luke.network.packet;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PacketTest {

  @Test
  public void testPacketStringConversion() {
    String message = "hello there!";
    assertThat(Packet.of(message).asString()).isEqualTo(message);
  }

  @Test
  public void testPacketEquals() {
    byte byte1 = 0b00000000;
    byte byte2 = 0b00000001;
    byte byte3 = 0b00000010;
    byte byte4 = 0b00000011;

    assertThat(new Packet(new byte[] {byte1, byte2, byte3, byte4}))
        .isEqualTo(new Packet(new byte[] {byte1, byte2, byte3, byte4}));
    assertThat(new Packet(new byte[] {byte1, byte2, byte3}))
        .isNotEqualTo(new Packet(new byte[] {byte1, byte2, byte3, byte4}));

    assertThat(new Packet(new byte[] {byte2, byte4}))
        .isEqualTo(new Packet(new byte[] {byte2, byte4}));
    assertThat(new Packet(new byte[] {byte2, byte3}))
        .isNotEqualTo(new Packet(new byte[] {byte2, byte4}));
  }
}
