package thorpe.luke.network.simulation.worker;

import java.nio.file.Path;
import java.time.LocalDateTime;

import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.NodeTopology;
import thorpe.luke.time.Tickable;
import thorpe.luke.time.TickableClock;

public class Worker implements Tickable {

  private final WorkerScript workerScript;
  private final WorkerAddress address;
  private final Mailbox mailbox;
  private final TickableClock clock;
  private final WorkerManager workerManager;

  public Worker(
          WorkerScript workerScript,
          WorkerAddress address,
          NodeTopology nodeTopology,
          Path crashDumpLocation,
          WorkerAddressGenerator workerAddressGenerator,
          WorkerAddressBook workerAddressBook, Mailbox mailbox,
          PostalService postalService) {
    this.workerScript = workerScript;
    this.address = address;
    this.mailbox = mailbox;
    this.clock = new TickableClock(null);
    this.workerManager = new WorkerManager(
            address,
            workerAddressBook, mailbox, postalService, crashDumpLocation, workerAddressGenerator, clock, nodeTopology
    );
  }

  @Override
  public void tick(LocalDateTime now) {
    clock.tick(now);
    workerScript.tick(workerManager);
  }

  public WorkerAddress getAddress() {
    return address;
  }

  public void post(Packet packet) {
    mailbox.post(packet);
  }

  public boolean isAlive() {
    return workerManager.isAlive();
  }
}
