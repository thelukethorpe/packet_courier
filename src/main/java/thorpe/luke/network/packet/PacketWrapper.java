package thorpe.luke.network.packet;

import java.util.function.Function;

public interface PacketWrapper<Wrapper extends PacketWrapper<Wrapper>> {
  Wrapper map(Function<Packet, Packet> function);

  Wrapper copy();

  Packet getPacket();
}
