package thorpe.luke.network.packet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class NeutralPacketFilterTest {

  @Test
  public void testEnqueueDequeueCycleIsFifo() {
    NeutralPacketFilter neutralPacketFilter = new NeutralPacketFilter();
    assertThat(neutralPacketFilter.tryDequeue()).isEmpty();
    Packet packet1 = Packet.of("Packet #1");
    Packet packet2 = Packet.of("Packet #2");
    Packet packet3 = Packet.of("Packet #3");
    neutralPacketFilter.enqueue(packet1);
    neutralPacketFilter.enqueue(packet2);
    neutralPacketFilter.enqueue(packet3);
    assertThat(neutralPacketFilter.tryDequeue()).hasValue(packet1);
    assertThat(neutralPacketFilter.tryDequeue()).hasValue(packet2);
    assertThat(neutralPacketFilter.tryDequeue()).hasValue(packet3);
    assertThat(neutralPacketFilter.tryDequeue()).isEmpty();
  }
}
