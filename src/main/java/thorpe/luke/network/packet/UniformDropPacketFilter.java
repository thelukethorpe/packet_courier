package thorpe.luke.network.packet;

import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.BernoulliDistribution;
import thorpe.luke.distribution.Distribution;

public class UniformDropPacketFilter implements PacketFilter {

  private final NeutralPacketFilter neutralPacketFilter = new NeutralPacketFilter();
  private final Distribution<Boolean> dropDistribution;

  public UniformDropPacketFilter(double dropProbability) {
    this(dropProbability, new Random());
  }

  public UniformDropPacketFilter(double dropProbability, Random random) {
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
