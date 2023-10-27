package thorpe.luke.network.simulation.worker;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.NodeTopology;
import thorpe.luke.util.GarbageCollector;
import thorpe.luke.util.Prunable;

public class WorkerAddressBook implements Prunable {

  private static final int PRUNE_PERIOD = 10;

  private final ConcurrentMap<WorkerAddress, Worker> addressToWorkerMap;
  private final GarbageCollector garbageCollector;

  public WorkerAddressBook() {
    this.addressToWorkerMap = new ConcurrentHashMap<>();
    this.garbageCollector = new GarbageCollector(this, PRUNE_PERIOD);
  }

  public Optional<Worker> lookup(WorkerAddress address) {
    garbageCollector.tick();
    return Optional.ofNullable(addressToWorkerMap.get(address));
  }

  public Worker registerWorker(
      WorkerScript workerScript,
      WorkerAddress workerAddress,
      NodeTopology nodeTopology,
      Path crashDumpLocation,
      WorkerAddressGenerator workerAddressGenerator,
      WorkerAddressBook workerAddressBook,
      PostalService postalService) {
    Worker worker =
        new Worker(
            workerScript,
            workerAddress,
            nodeTopology,
            crashDumpLocation,
            workerAddressGenerator,
            workerAddressBook,
            new Mailbox(),
            postalService);
    addressToWorkerMap.put(workerAddress, worker);
    return worker;
  }

  @Override
  public void prune() {
    addressToWorkerMap.values().removeIf(worker -> worker.getState() == Worker.State.DEAD);
  }
}
