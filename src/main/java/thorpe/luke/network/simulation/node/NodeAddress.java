package thorpe.luke.network.simulation.node;

import java.io.Serializable;
import thorpe.luke.network.simulation.worker.WorkerAddress;

public class NodeAddress implements Serializable {
  private final String name;

  public NodeAddress(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public WorkerAddress asRootWorkerAddress() {
    return new WorkerAddress("root", this);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NodeAddress) {
      NodeAddress that = (NodeAddress) obj;
      return this.name.equals(that.name);
    }
    return false;
  }

  @Override
  public String toString() {
    return "Node(" + name + ")";
  }
}
