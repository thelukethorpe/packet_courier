package thorpe.luke.network.packet;

import java.util.Optional;

public interface PacketFilter {
  void enqueue(Packet packet);

  Optional<Packet> tryDequeue();
}
