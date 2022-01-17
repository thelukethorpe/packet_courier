package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketLimitingFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final NeutralPacketFilter<Wrapper> neutralPacketFilter = new NeutralPacketFilter<>();
  private final int limit;
  private final AtomicInteger occupancy;

  public PacketLimitingFilter(int limit) {
    this.limit = limit;
    this.occupancy = new AtomicInteger(0);
  }

  @Override
  public void tick(LocalDateTime now) {
    // Do nothing.
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    int occupancy = this.occupancy.incrementAndGet();
    if (occupancy > limit) {
      this.occupancy.decrementAndGet();
      return;
    }
    neutralPacketFilter.enqueue(packetWrapper);
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    Optional<Wrapper> dequeueResult = neutralPacketFilter.tryDequeue();
    if (dequeueResult.isPresent()) {
      occupancy.decrementAndGet();
    }
    return dequeueResult;
  }
}
