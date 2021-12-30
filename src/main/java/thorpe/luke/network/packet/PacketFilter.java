package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PacketFilter {
  void tick(LocalDateTime now);

  void enqueue(Packet packet);

  Optional<Packet> tryDequeue();
}
