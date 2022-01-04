package thorpe.luke.network.simulator;

import java.util.Objects;

public class Connection<NodeInfo> {
  private final Node<NodeInfo> source;
  private final Node<NodeInfo> destination;

  public Connection(Node<NodeInfo> source, Node<NodeInfo> destination) {
    this.source = source;
    this.destination = destination;
  }

  public Node<NodeInfo> getSource() {
    return source;
  }

  public Node<NodeInfo> getDestination() {
    return destination;
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Connection) {
      Connection that = (Connection) obj;
      return this.source.equals(that.source) && this.destination.equals(that.destination);
    }
    return false;
  }
}
