package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.Distribution;

public class SimulatedPacketDropFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final NeutralPacketFilter<Wrapper> neutralPacketFilter = new NeutralPacketFilter<>();
  private final Distribution<Boolean> dropDistribution;
  private final Random random;

  public SimulatedPacketDropFilter(Distribution<Boolean> dropDistribution, Random random) {
    this.dropDistribution = dropDistribution;
    this.random = random;
  }

  @Override
  public void tick(LocalDateTime now) {
    // Do nothing.
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    if (!dropDistribution.sample(random)) {
      neutralPacketFilter.enqueue(packetWrapper);
    }
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return neutralPacketFilter.tryDequeue();
  }
}
