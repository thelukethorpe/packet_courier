package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class PacketLimitingFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final Queue<Wrapper> packetWrapperQueue = new LinkedList<>();
  private final int limit;

  public PacketLimitingFilter(int limit) {
    this.limit = limit;
  }

  @Override
  public void tick(LocalDateTime now) {
    // Do nothing.
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    if (packetWrapperQueue.size() < limit) {
      packetWrapperQueue.offer(packetWrapper);
    }
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return Optional.ofNullable(packetWrapperQueue.poll());
  }
}
