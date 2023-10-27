package thorpe.luke.network.simulation.node;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import thorpe.luke.network.simulation.mail.Mail;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.worker.*;
import thorpe.luke.time.Tickable;

public class Node implements Tickable {
  private final NodeAddress address;
  private final WorkerAddressGenerator workerAddressGenerator;
  private final Map<WorkerAddress, Worker> addressToWorkerMap = new HashMap<>();

  public Node(String name) {
    this(new NodeAddress(name));
  }

  private Node(NodeAddress address) {
    this.address = address;
    this.workerAddressGenerator = new WorkerAddressGenerator();
  }

  public void registerWorker(
      WorkerScript workerScript,
      NodeTopology nodeTopology,
      Path crashDumpLocation,
      PostalService postalService) {
    WorkerAddress workerAddress = address.asRootWorkerAddress();
    Worker worker =
            new Worker(
                    workerScript,
                    workerAddress,
                    nodeTopology,
                    crashDumpLocation,
                    workerAddressGenerator,
                    new Mailbox(),
                    postalService);
    addressToWorkerMap.put(workerAddress, worker);
  }

  @Override
  public void tick(LocalDateTime now) {
    addressToWorkerMap.values().forEach(worker -> worker.tick(now));
    addressToWorkerMap.values().removeIf(worker -> !worker.isAlive());
  }

  public NodeAddress getAddress() {
    return address;
  }

  private Optional<Worker> lookup(WorkerAddress address) {
    return Optional.ofNullable(addressToWorkerMap.get(address));
  }

  public void deliver(Mail mail) {
    lookup(mail.getDestinationAddress())
        .ifPresent(worker -> worker.post(mail.getPacket()));
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof Node) {
      Node that = (Node) obj;
      return this.address.equals(that.address);
    }
    return false;
  }
}
