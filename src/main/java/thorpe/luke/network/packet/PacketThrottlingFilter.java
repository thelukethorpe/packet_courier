package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PacketThrottlingFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final Queue<Wrapper> packetWrapperQueue = new ConcurrentLinkedQueue<>();
  private final Lock dequeueLock = new ReentrantLock();
  private final int throttleRate;
  private final ChronoUnit timeUnit;
  private final AtomicLong byteBudget;
  private LocalDateTime now;

  public PacketThrottlingFilter(int throttleRate, ChronoUnit timeUnit, LocalDateTime startTime) {
    this.throttleRate = throttleRate;
    this.byteBudget = new AtomicLong(0);
    this.timeUnit = timeUnit;
    this.now = startTime;
  }

  @Override
  public void tick(LocalDateTime now) {
    long timeElapsed = timeUnit.between(this.now, now);
    byteBudget.addAndGet(timeElapsed * throttleRate);
    this.now = now;
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    packetWrapperQueue.offer(packetWrapper);
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    dequeueLock.lock();
    try {
      Wrapper packetWrapper = packetWrapperQueue.peek();
      if (packetWrapper == null) {
        return Optional.empty();
      }
      int packetByteCount = packetWrapper.getPacket().countBytes();
      if (packetByteCount > byteBudget.get()) {
        return Optional.empty();
      }
      byteBudget.addAndGet(-packetByteCount);
      packetWrapperQueue.poll();
      return Optional.of(packetWrapper);
    } finally {
      dequeueLock.unlock();
    }
  }
}
