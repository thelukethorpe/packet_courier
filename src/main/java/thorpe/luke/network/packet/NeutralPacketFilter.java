package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class NeutralPacketFilter<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final Queue<Wrapper> packetWrapperQueue = new LinkedList<>();

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
