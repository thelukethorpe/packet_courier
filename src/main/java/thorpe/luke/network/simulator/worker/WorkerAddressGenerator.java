package thorpe.luke.network.simulator.worker;

import thorpe.luke.network.simulator.node.NodeAddress;
import thorpe.luke.util.UniqueStringGenerator;

public class WorkerAddressGenerator {
  private final NodeAddress hostingNodeAddress;
  private final UniqueStringGenerator uniqueStringGenerator;

  public WorkerAddressGenerator(NodeAddress hostingNodeAddress) {
    this.hostingNodeAddress = hostingNodeAddress;
    this.uniqueStringGenerator = new UniqueStringGenerator();
  }

  private String generateUniqueName() {
    String uniqueString = uniqueStringGenerator.generateUniqueString();
    return "Worker(" + uniqueString + ") running on Node(" + hostingNodeAddress.getName() + ")";
  }

  public WorkerAddress generateUniqueWorkerAddress() {
    return new WorkerAddress(generateUniqueName(), hostingNodeAddress);
  }

  public WorkerAddress generateUniqueChildWorkerAddress(WorkerAddress parentWorkerAddress) {
    return parentWorkerAddress.createChildWorkerAddress(generateUniqueName());
  }
}
