package thorpe.luke.network.simulation.worker;

@FunctionalInterface
public interface WorkerScript<NodeInfo> {
  void run(WorkerManager<NodeInfo> workerManager);
}
