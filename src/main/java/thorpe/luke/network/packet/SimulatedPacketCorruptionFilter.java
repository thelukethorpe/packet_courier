package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.Distribution;

public class SimulatedPacketCorruptionFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final NeutralPacketFilter<Wrapper> neutralPacketFilter = new NeutralPacketFilter<>();
  private final Distribution<Boolean> corruptionDistribution;
  private final Distribution<Integer> byteCorruptionDistribution;
  private final Distribution<Integer> bitFlippingDistribution;
  private final Random random;

  public SimulatedPacketCorruptionFilter(
      Distribution<Boolean> corruptionDistribution,
      Distribution<Integer> byteCorruptionDistribution,
      Distribution<Integer> bitFlippingDistribution,
      Random random) {
    this.corruptionDistribution = corruptionDistribution;
    this.byteCorruptionDistribution = byteCorruptionDistribution;
    this.bitFlippingDistribution = bitFlippingDistribution;
    this.random = random;
  }

  @Override
  public void tick(LocalDateTime now) {
    // Do nothing.
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    if (corruptionDistribution.sample(random)) {
      packetWrapper = corrupt(packetWrapper);
    }
    neutralPacketFilter.enqueue(packetWrapper);
  }

  private Wrapper corrupt(Wrapper packetWrapper) {
    return packetWrapper.map(
        packet -> {
          List<Byte> bytes = packet.getData();
          int byteCorruptionIndex = byteCorruptionDistribution.sample(random) % bytes.size();
          int bitFlippingIndex = bitFlippingDistribution.sample(random) & (Byte.SIZE - 1);
          int bitFlippingMask = 1 << bitFlippingIndex;
          byte byteToCorrupt = bytes.get(byteCorruptionIndex);
          bytes.set(byteCorruptionIndex, (byte) (byteToCorrupt ^ bitFlippingMask));
          return new Packet(bytes);
        });
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return neutralPacketFilter.tryDequeue();
  }
}
