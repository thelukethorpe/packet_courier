package thorpe.luke.network.simulator.node;

import thorpe.luke.network.simulator.worker.WorkerAddress;

public class NodeAddress {
  private final String name;

  public NodeAddress(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public WorkerAddress asRootWorkerAddress() {
    return new WorkerAddress("Worker(root) running on Node(" + name + ")", this);
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
