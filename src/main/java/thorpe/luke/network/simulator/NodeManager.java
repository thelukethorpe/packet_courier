package thorpe.luke.network.simulator;

import java.util.Collection;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.Packet;

public class NodeManager<NodeInfo> {
  private final NodeInfo nodeInfo;
  private final String name;
  private final Mailbox mailbox;
  private final PostalService postalService;
  private final Collection<Logger> loggers;

  public NodeManager(
      NodeInfo nodeInfo,
      String name,
      Mailbox mailbox,
      PostalService postalService,
      Collection<Logger> loggers) {
    this.nodeInfo = nodeInfo;
    this.name = name;
    this.mailbox = mailbox;
    this.postalService = postalService;
    this.loggers = loggers;
  }

  public NodeInfo getInfo() {
    return nodeInfo;
  }

  public void sendMail(String destinationAddress, Packet packet) {
    postalService.mail(name, destinationAddress, packet);
  }

  public Packet waitForMail() throws InterruptedException {
    return mailbox.waitForMail();
  }

  public void log(String message) {
    loggers.forEach(logger -> logger.log(message));
  }
}
