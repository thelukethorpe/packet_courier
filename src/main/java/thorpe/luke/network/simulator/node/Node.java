package thorpe.luke.network.simulator.node;

import java.util.Collection;
import thorpe.luke.log.Logger;
import thorpe.luke.network.simulator.mail.Mail;
import thorpe.luke.network.simulator.mail.PostalService;
import thorpe.luke.network.simulator.worker.*;

public class Node<NodeInfo> {
  private final NodeAddress address;
  private final WorkerAddressGenerator workerAddressGenerator;
  private final WorkerAddressBook<NodeInfo> workerAddressBook;

  public Node(String name) {
    this(new NodeAddress(name));
  }

  private Node(NodeAddress address) {
    this.address = address;
    this.workerAddressGenerator = new WorkerAddressGenerator(address);
    this.workerAddressBook = new WorkerAddressBook<>();
  }

  public void doWork(
      WorkerScript<NodeInfo> workerScript,
      NodeInfo nodeInfo,
      PostalService postalService,
      Collection<Logger> loggers) {
    WorkerAddress workerAddress = address.asRootWorkerAddress();
    Worker<NodeInfo> worker =
        workerAddressBook.registerWorker(
            workerScript,
            workerAddress,
            nodeInfo,
            postalService,
            loggers,
            workerAddressGenerator,
            workerAddressBook);
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
