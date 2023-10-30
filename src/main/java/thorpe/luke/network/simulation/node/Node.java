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
  private final WorkerAddressBook workerAddressBook = new WorkerAddressBook();
  private final WorkerAddressGenerator workerAddressGenerator = new WorkerAddressGenerator();

  public Node(String name) {
    this(new NodeAddress(name));
  }

  private Node(NodeAddress address) {
    this.address = address;
  }

  public void registerWorker(
          WorkerScript workerScript,
          NodeTopology nodeTopology,
          Path crashDumpLocation,
          PostalService postalService) {
    WorkerAddress workerAddress = workerAddressGenerator.generateUniqueChildWorkerAddress(address.asRootWorkerAddress());
    workerAddressBook.registerWorker(workerScript, workerAddress, nodeTopology, crashDumpLocation, workerAddressGenerator, postalService);
  }

public boolean hasActiveWorkers() {
    return workerAddressBook.hasActiveWorkers();
}

  @Override
  public void tick(LocalDateTime now) {
    workerAddressBook.forEachWorker(worker -> worker.tick(now));
    workerAddressBook.removeDeadWorkers();
  }

  public NodeAddress getAddress() {
    return address;
  }

  public void deliver(Mail mail) {
    workerAddressBook.lookup(mail.getDestinationAddress())
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
