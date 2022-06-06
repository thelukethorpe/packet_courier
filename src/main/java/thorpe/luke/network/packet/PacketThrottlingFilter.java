package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class PacketThrottlingFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final PacketLatencyFilter<Wrapper> packetLatencyFilter;
  private final int byteThrottleRate;
  private final long timeDropThreshold;
  private final ChronoUnit timeUnit;
  private double timeDelta;
  private LocalDateTime now;
  private LocalDateTime previousScheduledDequeueTime;

  public PacketThrottlingFilter(
      int byteThrottleRate, int byteDropThreshold, ChronoUnit timeUnit, LocalDateTime startTime) {
    this.packetLatencyFilter = new PacketLatencyFilter<>(startTime);
    this.byteThrottleRate = byteThrottleRate;
    this.timeDropThreshold = byteDropThreshold / byteThrottleRate;
    this.timeUnit = timeUnit;
    this.timeDelta = 0.0;
    this.now = startTime;
    this.previousScheduledDequeueTime = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    if (previousScheduledDequeueTime.isBefore(now)) {
      previousScheduledDequeueTime = now;
      timeDelta = 0.0;
    }
    this.now = now;
    packetLatencyFilter.tick(now);
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    if (timeUnit.between(now, previousScheduledDequeueTime) > timeDropThreshold) {
      return;
    }
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
