package thorpe.luke.network.simulator.mail;

import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulator.worker.WorkerAddress;

public interface PostalService {

  boolean mail(WorkerAddress sourceAddress, WorkerAddress destinationAddress, Packet packet);
}
