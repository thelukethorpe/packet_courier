package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.Distribution;

public class SimulatedPacketDuplicationFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final NeutralPacketFilter<Wrapper> neutralPacketFilter = new NeutralPacketFilter<>();
  private final Distribution<Integer> duplicationDistribution;
  private final Random random;

  public SimulatedPacketDuplicationFilter(
      Distribution<Integer> duplicationDistribution, Random random) {
    this.duplicationDistribution = duplicationDistribution;
    this.random = random;
  }

  @Override
  public void tick(LocalDateTime now) {
    // Do nothing.
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    neutralPacketFilter.enqueue(packetWrapper);
    int numberOfDuplicatePackets = duplicationDistribution.sample(random);
    for (int i = 0; i < numberOfDuplicatePackets; i++) {
      neutralPacketFilter.enqueue(packetWrapper.copy());
    }
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return neutralPacketFilter.tryDequeue();
  }
}
