package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Queue;

abstract class AbstractNeutralPacketFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final Queue<Wrapper> packetWrapperQueue;

  protected AbstractNeutralPacketFilter(Queue<Wrapper> packetWrapperQueue) {
    this.packetWrapperQueue = packetWrapperQueue;
  }

  @Override
  public void tick(LocalDateTime now) {
    // Do nothing.
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    packetWrapperQueue.offer(packetWrapper);
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    return Optional.ofNullable(packetWrapperQueue.poll());
  }
}
