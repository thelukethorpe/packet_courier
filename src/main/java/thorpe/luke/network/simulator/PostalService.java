package thorpe.luke.network.simulator;

import thorpe.luke.network.packet.Packet;

public interface PostalService {

  boolean mail(String sourceAddress, String destinationAddress, Packet packet);
}
