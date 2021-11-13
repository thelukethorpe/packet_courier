package thorpe.luke.network.packet;

import java.util.Optional;
import thorpe.luke.distribution.Distribution;

public class SimulatedPacketDropFilter implements PacketFilter {

  private final NeutralPacketFilter neutralPacketFilter = new NeutralPacketFilter();
  private final Distribution<Boolean> dropDistribution;

  public SimulatedPacketDropFilter(Distribution<Boolean> dropDistribution) {
    this.dropDistribution = dropDistribution;
  }

  @Override
  public void enqueue(Packet packet) {
    if (!dropDistribution.sample()) {
      neutralPacketFilter.enqueue(packet);
    }
  }

  @Override
  public Optional<Packet> tryDequeue() {
    return neutralPacketFilter.tryDequeue();
  }
}
