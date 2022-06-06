package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.PriorityQueue;

public class PacketLatencyFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<ScheduledPacket<Wrapper>> {

  private final PriorityQueue<ScheduledPacket<Wrapper>> scheduledPacketQueue =
      new PriorityQueue<>();
  private LocalDateTime now;

  public PacketLatencyFilter(LocalDateTime startTime) {
    this.now = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    this.now = now;
  }

  @Override
  public void enqueue(ScheduledPacket<Wrapper> scheduledPacket) {
    scheduledPacketQueue.offer(scheduledPacket);
  }

  @Override
  public Optional<ScheduledPacket<Wrapper>> tryDequeue() {
    ScheduledPacket<Wrapper> scheduledPacket = scheduledPacketQueue.peek();
    if (scheduledPacket == null || scheduledPacket.getScheduledDequeueTime().isAfter(now)) {
      return Optional.empty();
    }
    scheduledPacketQueue.poll();
    return Optional.of(scheduledPacket);
  }
}
