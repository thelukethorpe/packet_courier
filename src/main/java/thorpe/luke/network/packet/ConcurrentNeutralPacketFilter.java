package thorpe.luke.network.packet;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentNeutralPacketFilter<Wrapper extends PacketWrapper<Wrapper>>
    extends AbstractNeutralPacketFilter<Wrapper> {

  protected ConcurrentNeutralPacketFilter() {
    super(new ConcurrentLinkedQueue<>());
  }
}
