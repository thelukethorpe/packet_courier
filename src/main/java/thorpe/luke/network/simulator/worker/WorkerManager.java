package thorpe.luke.network.simulator.worker;

import java.util.Collection;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulator.mail.Mailbox;
import thorpe.luke.network.simulator.mail.PostalService;

public class WorkerManager<NodeInfo> {
  private final WorkerAddress address;
  private final NodeInfo nodeInfo;
  private final Mailbox mailbox;
  private final PostalService postalService;
  private final Collection<Logger> loggers;
  private final WorkerAddressGenerator workerAddressGenerator;
  private final WorkerAddressBook<NodeInfo> workerAddressBook;

  public WorkerManager(
      WorkerAddress address,
      NodeInfo nodeInfo,
      Mailbox mailbox,
      PostalService postalService,
      Collection<Logger> loggers,
      WorkerAddressGenerator workerAddressGenerator,
      WorkerAddressBook<NodeInfo> workerAddressBook) {
    this.address = address;
    this.nodeInfo = nodeInfo;
    this.mailbox = mailbox;
    this.postalService = postalService;
    this.loggers = loggers;
    this.workerAddressGenerator = workerAddressGenerator;
    this.workerAddressBook = workerAddressBook;
  }

  public WorkerAddress getAddress() {
    return address;
  }

  public NodeInfo getInfo() {
    return nodeInfo;
  }

  public void sendMail(WorkerAddress destinationAddress, Packet packet) {
    postalService.mail(this.address, destinationAddress, packet);
  }

  public Packet waitForMail() {
    return mailbox.waitForMail();
  }

  public Worker<NodeInfo> spawnChildWorker(WorkerScript<NodeInfo> workerScript) {
    WorkerAddress childWorkerAddress =
        workerAddressGenerator.generateUniqueChildWorkerAddress(address);
    return workerAddressBook.registerWorker(
        workerScript,
        childWorkerAddress,
        nodeInfo,
        postalService,
        loggers,
        workerAddressGenerator,
        workerAddressBook);
  }

  public void log(String message) {
    loggers.forEach(logger -> logger.log(message));
  }
}
