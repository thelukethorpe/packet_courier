package thorpe.luke.network.packet;

import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.BernoulliDistribution;
import thorpe.luke.distribution.Distribution;

public class SimulatedDropPacketFilter implements PacketFilter {

  private final NeutralPacketFilter neutralPacketFilter = new NeutralPacketFilter();
  private final Distribution<Boolean> dropDistribution;

  public SimulatedDropPacketFilter(double dropProbability) {
    this(dropProbability, new Random());
  }

  public SimulatedDropPacketFilter(double dropProbability, Random random) {
    this.dropDistribution = new BernoulliDistribution(dropProbability, random);
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
