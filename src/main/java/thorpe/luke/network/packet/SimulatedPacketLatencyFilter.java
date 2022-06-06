package thorpe.luke.network.packet;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import thorpe.luke.distribution.Distribution;

public class SimulatedPacketLatencyFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final PacketLatencyFilter<Wrapper> packetLatencyFilter;
  private final Distribution<Double> latencyDistribution;
  private final ChronoUnit timeUnit;
  private final Random random;
  private LocalDateTime now;

  public SimulatedPacketLatencyFilter(
      Distribution<Double> latencyDistribution,
      ChronoUnit timeUnit,
      LocalDateTime startTime,
      Random random) {
    this.packetLatencyFilter = new PacketLatencyFilter<>(startTime);
    this.latencyDistribution = latencyDistribution;
    this.timeUnit = timeUnit;
    this.random = random;
    this.now = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    this.now = now;
    packetLatencyFilter.tick(now);
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    long latency = Math.round(latencyDistribution.sample(random));
    LocalDateTime scheduledDequeueTime = now.plus(Duration.of(latency, timeUnit));
    packetLatencyFilter.enqueue(new ScheduledPacket<>(scheduledDequeueTime, packetWrapper));
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return packetLatencyFilter.tryDequeue().map(ScheduledPacket::getPacketWrapper);
  }
}
