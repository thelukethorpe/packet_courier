package thorpe.luke.network.packet;

import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.Distribution;

public class SimulatedPacketDropFilter implements PacketFilter {

  private final NeutralPacketFilter neutralPacketFilter = new NeutralPacketFilter();
  private final Distribution<Boolean> dropDistribution;
  private final Random random;

  public SimulatedPacketDropFilter(Distribution<Boolean> dropDistribution, Random random) {
    this.dropDistribution = dropDistribution;
    this.random = random;
  }

  @Override
  public void enqueue(Packet packet) {
    if (!dropDistribution.sample(random)) {
      neutralPacketFilter.enqueue(packet);
    }
  }

  @Override
  public Optional<Packet> tryDequeue() {
    return neutralPacketFilter.tryDequeue();
  }
}
