package thorpe.luke.network.packet;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.PriorityQueue;
import thorpe.luke.distribution.Distribution;

public class SimulatedPacketLatencyFilter implements PacketFilter {

  private final PriorityQueue<ScheduledPacket> packetQueue = new PriorityQueue<>();
  private final Distribution<Double> latencyDistribution;
  private final ChronoUnit timeUnit;

  public SimulatedPacketLatencyFilter(
      Distribution<Double> latencyDistribution, ChronoUnit timeUnit) {
    this.latencyDistribution = latencyDistribution;
    this.timeUnit = timeUnit;
  }

  @Override
  public void enqueue(Packet packet) {
    LocalDateTime now = LocalDateTime.now();
    long latency = Math.round(latencyDistribution.sample());
    LocalDateTime scheduledDequeueTime = now.plus(Duration.of(latency, timeUnit));
    packetQueue.offer(new ScheduledPacket(scheduledDequeueTime, packet));
  }

  @Override
  public Optional<Packet> tryDequeue() {
    LocalDateTime now = LocalDateTime.now();
    if (packetQueue.isEmpty() || packetQueue.peek().getScheduledDequeueTime().isAfter(now)) {
      return Optional.empty();
    }
    return Optional.ofNullable(packetQueue.poll()).map(ScheduledPacket::getPacket);
  }

  private static class ScheduledPacket implements Comparable<ScheduledPacket> {

    private final LocalDateTime scheduledDequeueTime;
    private final Packet packet;

    private ScheduledPacket(LocalDateTime scheduledDequeueTime, Packet packet) {
      this.scheduledDequeueTime = scheduledDequeueTime;
      this.packet = packet;
    }

    public LocalDateTime getScheduledDequeueTime() {
      return scheduledDequeueTime;
    }

    public Packet getPacket() {
      return packet;
    }

    @Override
    public int compareTo(ScheduledPacket that) {
      return this.scheduledDequeueTime.compareTo(that.scheduledDequeueTime);
    }
  }
}
