package thorpe.luke.network.packet;

import java.util.Optional;
import java.util.Random;

public class UniformDropPacketFilter implements PacketFilter {

  private final NeutralPacketFilter neutralPacketFilter = new NeutralPacketFilter();
  private final double dropProbability;
  private final Random random;

  public UniformDropPacketFilter(double dropProbability) {
    this(dropProbability, new Random());
  }

  public UniformDropPacketFilter(double dropProbability, long seed) {
    this(dropProbability, new Random(seed));
  }

  private UniformDropPacketFilter(double dropProbability, Random random) {
    assert (0.0 <= dropProbability && dropProbability <= 1.0);
    this.dropProbability = dropProbability;
    this.random = random;
  }

  @Override
  public void enqueue(Packet packet) {
    if (dropProbability <= random.nextDouble()) {
      neutralPacketFilter.enqueue(packet);
    }
  }

  @Override
  public Optional<Packet> tryDequeue() {
    return neutralPacketFilter.tryDequeue();
  }
}
