package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class PacketThrottlingFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final Queue<Wrapper> packetWrapperQueue = new LinkedList<>();
  private final int byteThrottleRate;
  private final ChronoUnit timeUnit;
  private long byteBudget;
  private LocalDateTime now;

  public PacketThrottlingFilter(
      int byteThrottleRate, ChronoUnit timeUnit, LocalDateTime startTime) {
    this.byteThrottleRate = byteThrottleRate;
    this.timeUnit = timeUnit;
    this.byteBudget = 0;
    this.now = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    long timeElapsed = timeUnit.between(this.now, now);
    byteBudget += timeElapsed * byteThrottleRate;
    this.now = now;
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    packetWrapperQueue.offer(packetWrapper);
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    Wrapper packetWrapper = packetWrapperQueue.peek();
    if (packetWrapper == null) {
      return Optional.empty();
    }
    int packetByteCount = packetWrapper.getPacket().countBytes();
    if (packetByteCount > byteBudget) {
      return Optional.empty();
    }
    byteBudget -= packetByteCount;
    packetWrapperQueue.poll();
    return Optional.of(packetWrapper);
  }
}
