package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class PacketLimitingFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final Queue<Wrapper> packetWrapperQueue = new LinkedList<>();
  private final int packetLimitRate;
  private final ChronoUnit timeUnit;
  private long packetBudget;
  private LocalDateTime now;

  public PacketLimitingFilter(int packetLimitRate, ChronoUnit timeUnit, LocalDateTime startTime) {
    this.packetLimitRate = packetLimitRate;
    this.timeUnit = timeUnit;
    this.packetBudget = 0;
    this.now = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    long timeElapsed = timeUnit.between(this.now, now);
    if (timeElapsed > 0) {
      packetBudget = timeElapsed * packetLimitRate;
      this.now = now;
    }
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    if (packetBudget > 0) {
      packetBudget--;
      packetWrapperQueue.offer(packetWrapper);
    }
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return Optional.ofNullable(packetWrapperQueue.poll());
  }
}
