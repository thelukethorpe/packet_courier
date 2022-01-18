package thorpe.luke.network.packet;

import java.util.LinkedList;

public class NeutralPacketFilter<Wrapper extends PacketWrapper<Wrapper>>
    extends AbstractNeutralPacketFilter<Wrapper> {

  protected NeutralPacketFilter() {
    super(new LinkedList<>());
  }
}
