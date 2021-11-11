package thorpe.luke.network.packet;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class NeutralPacketFilter implements PacketFilter {

  private final Queue<Packet> packets = new LinkedList<>();

  @Override
  public void enqueue(Packet packet) {
    packets.offer(packet);
  }

  @Override
  public Optional<Packet> tryDequeue() {
    return Optional.ofNullable(packets.poll());
  }
}
