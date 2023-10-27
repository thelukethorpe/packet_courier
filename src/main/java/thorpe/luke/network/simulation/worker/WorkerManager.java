package thorpe.luke.network.simulation.worker;

import java.nio.file.Path;
import java.time.LocalDateTime;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.NodeTopology;
import thorpe.luke.time.Clock;

public class WorkerManager {

  private final WorkerAddress address;
  private final Clock clock;
  private final NodeTopology nodeTopology;
  private final Mailbox mailbox;
  private final PostalService postalService;
  private final Path crashDumpLocation;
  private final WorkerAddressGenerator workerAddressGenerator;
  private final WorkerAddressBook workerAddressBook;

  public WorkerManager(
      WorkerAddress address,
      Clock clock,
      NodeTopology nodeTopology,
      Mailbox mailbox,
      PostalService postalService,
      Path crashDumpLocation,
      WorkerAddressGenerator workerAddressGenerator,
      WorkerAddressBook workerAddressBook) {
    this.address = address;
    this.clock = clock;
    this.nodeTopology = nodeTopology;
    this.mailbox = mailbox;
    this.postalService = postalService;
    this.crashDumpLocation = crashDumpLocation;
    this.workerAddressGenerator = workerAddressGenerator;
    this.workerAddressBook = workerAddressBook;
  }

  public WorkerAddress getAddress() {
    return address;
  }

  public LocalDateTime getCurrentTime() {
    return clock.now();
  }

  public NodeTopology getTopology() {
    return nodeTopology;
  }

  public void sendMail(WorkerAddress destinationAddress, Packet packet) {
    postalService.mail(this.address, destinationAddress, packet);
  }

  public Packet waitForMail() {
    try {
      return mailbox.waitForMail();
    } catch (InterruptedException e) {
      throw new WorkerException(e);
    }
  }

  public Worker spawnChildWorker(WorkerScript workerScript) {
    WorkerAddress childWorkerAddress =
        workerAddressGenerator.generateUniqueChildWorkerAddress(address);
    return workerAddressBook.registerWorker(
        workerScript,
        childWorkerAddress,
        clock,
        nodeTopology,
        crashDumpLocation,
        workerAddressGenerator,
        workerAddressBook,
        postalService);
  }
}
