package thorpe.luke.network.simulator;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.time.Clock;

public class NodeManager {
  private final String name;
  private final Collection<String> neighbours;
  private final Mailbox mailbox;
  private final PostalService postalService;
  private final Clock clock;

  public NodeManager(
      String name,
      Collection<String> neighbours,
      Mailbox mailbox,
      PostalService postalService,
      Clock clock) {
    this.name = name;
    this.neighbours = Collections.unmodifiableCollection(neighbours);
    this.postalService = postalService;
    this.mailbox = mailbox;
    this.clock = clock;
  }

  public String getName() {
    return name;
  }

  public Collection<String> getNeighbours() {
    return neighbours;
  }

  public LocalDateTime getCurrentTime() {
    return clock.now();
  }

  public void sendMail(String destinationAddress, Packet packet) {
    postalService.mail(name, destinationAddress, packet);
  }

  public Packet waitForMail() throws InterruptedException {
    return mailbox.waitForMail();
  }
}
