package thorpe.luke.network.packet;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.Distribution;
import thorpe.luke.util.concurrent.ConcurrentLinkedPriorityQueue;

public class SimulatedPacketLatencyFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final ConcurrentLinkedPriorityQueue<ScheduledPacket> packetQueue =
      new ConcurrentLinkedPriorityQueue<>();
  private final Distribution<Double> latencyDistribution;
  private final ChronoUnit timeUnit;
  private final Random random;
  private LocalDateTime now;

  public SimulatedPacketLatencyFilter(
      Distribution<Double> latencyDistribution,
      ChronoUnit timeUnit,
      LocalDateTime startTime,
      Random random) {
    this.latencyDistribution = latencyDistribution;
    this.timeUnit = timeUnit;
    this.random = random;
    this.now = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    this.now = now;
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    long latency = Math.round(latencyDistribution.sample(random));
    LocalDateTime scheduledDequeueTime = now.plus(Duration.of(latency, timeUnit));
    packetQueue.offer(new ScheduledPacket(scheduledDequeueTime, packetWrapper));
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return Optional.ofNullable(
            packetQueue.pollIf(front -> front.getScheduledDequeueTime().isAfter(now)))
        .map(ScheduledPacket::getPacketWrapper);
  }

  private class ScheduledPacket implements Comparable<ScheduledPacket> {

    private final LocalDateTime scheduledDequeueTime;
    private final Wrapper packetWrapper;

    private ScheduledPacket(LocalDateTime scheduledDequeueTime, Wrapper packetWrapper) {
      this.scheduledDequeueTime = scheduledDequeueTime;
      this.packetWrapper = packetWrapper;
    }

    public LocalDateTime getScheduledDequeueTime() {
      return scheduledDequeueTime;
    }

    public Wrapper getPacketWrapper() {
      return packetWrapper;
    }

    @Override
    public int compareTo(ScheduledPacket that) {
      return this.scheduledDequeueTime.compareTo(that.scheduledDequeueTime);
    }
  }
}
