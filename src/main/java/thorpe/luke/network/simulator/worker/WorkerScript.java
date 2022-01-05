package thorpe.luke.network.simulator.worker;

@FunctionalInterface
public interface WorkerScript<NodeInfo> {
  void run(WorkerManager<NodeInfo> workerManager);
}
