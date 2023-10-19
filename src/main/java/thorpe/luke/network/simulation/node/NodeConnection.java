package thorpe.luke.network.simulation.node;

import java.util.Objects;

public class NodeConnection {
  private final Node source;
  private final Node destination;

  public NodeConnection(Node source, Node destination) {
    this.source = source;
    this.destination = destination;
  }

  public Node getSource() {
    return source;
  }

  public Node getDestination() {
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
    } else if (obj instanceof NodeConnection) {
      NodeConnection that = (NodeConnection) obj;
      return this.source.equals(that.source) && this.destination.equals(that.destination);
    }
    return false;
  }
}
