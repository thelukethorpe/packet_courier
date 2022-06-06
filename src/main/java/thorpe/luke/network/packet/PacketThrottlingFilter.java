package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class PacketThrottlingFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final PacketLatencyFilter<Wrapper> packetLatencyFilter;
  private final int byteThrottleRate;
  private final ChronoUnit timeUnit;
  private double timeDelta;
  private LocalDateTime previousScheduledDequeueTime;

  public PacketThrottlingFilter(
      int byteThrottleRate, ChronoUnit timeUnit, LocalDateTime startTime) {
    this.packetLatencyFilter = new PacketLatencyFilter<>(startTime);
    this.byteThrottleRate = byteThrottleRate;
    this.timeUnit = timeUnit;
    this.timeDelta = 0.0;
    this.previousScheduledDequeueTime = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    if (previousScheduledDequeueTime.isBefore(now)) {
      previousScheduledDequeueTime = now;
    }
    packetLatencyFilter.tick(now);
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    int packetSizeInBytes = packetWrapper.getPacket().countBytes();
    double timeRequiredToProcessPacket = packetSizeInBytes / (double) byteThrottleRate;
    long processingLatency = (long) timeRequiredToProcessPacket;
    timeDelta += timeRequiredToProcessPacket - processingLatency;
    if (timeDelta >= 1.0) {
      long latencyDelta = (long) timeDelta;
      processingLatency += latencyDelta;
      timeDelta -= latencyDelta;
    }
    LocalDateTime scheduledDequeueTime =
        previousScheduledDequeueTime.plus(processingLatency, timeUnit);
    packetLatencyFilter.enqueue(new ScheduledPacket<>(scheduledDequeueTime, packetWrapper));
    previousScheduledDequeueTime = scheduledDequeueTime;
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return packetLatencyFilter.tryDequeue().map(ScheduledPacket::getPacketWrapper);
  }
}
