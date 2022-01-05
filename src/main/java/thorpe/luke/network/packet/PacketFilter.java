package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PacketFilter<Wrapper extends PacketWrapper<Wrapper>> {
  void tick(LocalDateTime now);

  void enqueue(Wrapper packetWrapper);

  Optional<Wrapper> tryDequeue();
}
