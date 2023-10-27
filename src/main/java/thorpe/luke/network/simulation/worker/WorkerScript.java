package thorpe.luke.network.simulation.worker;

@FunctionalInterface
public interface WorkerScript {
  void tick(WorkerManager workerManager);
}
