package thorpe.luke.network.simulation.mail;

import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulation.worker.WorkerAddress;

public interface PostalService {

  boolean mail(WorkerAddress sourceAddress, WorkerAddress destinationAddress, Packet packet);
}
