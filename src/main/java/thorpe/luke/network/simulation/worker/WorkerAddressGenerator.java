package thorpe.luke.network.simulation.worker;

import thorpe.luke.util.UniqueStringGenerator;

public class WorkerAddressGenerator {
  private final UniqueStringGenerator uniqueStringGenerator;

  public WorkerAddressGenerator() {
    this.uniqueStringGenerator = new UniqueStringGenerator();
  }

  public WorkerAddress generateUniqueChildWorkerAddress(WorkerAddress parentWorkerAddress) {
    return parentWorkerAddress.createChildWorkerAddress(
        uniqueStringGenerator.generateUniqueString());
  }
}
