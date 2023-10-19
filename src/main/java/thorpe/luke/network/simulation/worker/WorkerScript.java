package thorpe.luke.network.simulation.worker;

@FunctionalInterface
public interface WorkerScript {
  void run(WorkerManager workerManager);
}
