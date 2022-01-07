package thorpe.luke.network.simulation.worker;

import java.io.Serializable;
import java.util.Objects;
import thorpe.luke.network.simulation.node.NodeAddress;

public class WorkerAddress implements Serializable {
  private final String name;
  private final NodeAddress hostingNodeAddress;
  private final WorkerAddress parentWorkerAddress;

  public WorkerAddress(String name, NodeAddress hostingNodeAddress) {
    this(name, hostingNodeAddress, null);
  }

  private WorkerAddress(
      String name, NodeAddress hostingNodeAddress, WorkerAddress parentWorkerAddress) {
    this.name = name;
    this.hostingNodeAddress = hostingNodeAddress;
    this.parentWorkerAddress = parentWorkerAddress;
  }

  public String getName() {
    return name;
  }

  public NodeAddress getHostingNodeAddress() {
    return hostingNodeAddress;
  }

  public WorkerAddress getParentWorkerAddress() {
    return parentWorkerAddress;
  }

  public WorkerAddress createChildWorkerAddress(String childName) {
    return new WorkerAddress(childName, hostingNodeAddress, this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, hostingNodeAddress, parentWorkerAddress);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof WorkerAddress) {
      WorkerAddress that = (WorkerAddress) obj;
      return this.name.equals(that.name)
          && this.hostingNodeAddress.equals(that.hostingNodeAddress)
          && Objects.equals(this.parentWorkerAddress, that.parentWorkerAddress);
    }
    return false;
  }

  @Override
  public String toString() {
    return "Worker(" + name + ") running on " + hostingNodeAddress;
  }
}
