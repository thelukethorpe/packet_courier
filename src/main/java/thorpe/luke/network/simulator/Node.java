package thorpe.luke.network.simulator;

import java.util.Collection;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.time.Clock;

public class Node {
  private final String name;
  private final Mailbox mailbox;

  public Node(String name) {
    this.name = name;
    this.mailbox = new Mailbox();
  }

  public String getName() {
    return name;
  }

  public void deliver(Packet packet) {
    mailbox.post(packet);
  }

  public void run(
      NodeScript nodeScript,
      Collection<String> neighbours,
      PostalService postalService,
      Clock clock) {
    nodeScript.run(new NodeManager(name, neighbours, mailbox, postalService, clock));
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof Node) {
      Node that = (Node) obj;
      return this.name.equals(that.name);
    }
    return false;
  }
}
