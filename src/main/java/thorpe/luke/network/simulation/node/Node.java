package thorpe.luke.network.simulation.node;

import java.nio.file.Path;
import thorpe.luke.log.Logger;
import thorpe.luke.network.simulation.Topology;
import thorpe.luke.network.simulation.mail.Mail;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.worker.*;
import thorpe.luke.time.Clock;
import thorpe.luke.util.error.ExceptionListener;

public class Node {
  private final NodeAddress address;
  private final WorkerAddressGenerator workerAddressGenerator;
  private final WorkerAddressBook workerAddressBook;

  public Node(String name) {
    this(new NodeAddress(name));
  }

  private Node(NodeAddress address) {
    this.address = address;
    this.workerAddressGenerator = new WorkerAddressGenerator();
    this.workerAddressBook = new WorkerAddressBook();
  }

  public void doWork(
      WorkerScript workerScript,
      Clock clock,
      Topology topology,
      ExceptionListener exceptionListener,
      Path crashDumpLocation,
      PostalService postalService,
      Logger logger) {
    WorkerAddress workerAddress = address.asRootWorkerAddress();
    Worker worker =
        workerAddressBook.registerWorker(
            workerScript,
            workerAddress,
            clock,
            topology,
            exceptionListener,
            crashDumpLocation,
            workerAddressGenerator,
            workerAddressBook,
            postalService,
            logger);
    worker.run();
  }

  public NodeAddress getAddress() {
    return address;
  }

  public void deliver(Mail mail) {
    workerAddressBook
        .lookup(mail.getDestinationAddress())
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
