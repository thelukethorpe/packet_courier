package thorpe.luke.network.simulation.worker;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import thorpe.luke.log.Logger;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.util.GarbageCollector;
import thorpe.luke.util.Prunable;

public class WorkerAddressBook<NodeInfo> implements Prunable {

  private static final int PRUNE_PERIOD = 10;

  private final ConcurrentMap<WorkerAddress, Worker<NodeInfo>> addressToWorkerMap;
  private final GarbageCollector garbageCollector;

  public WorkerAddressBook() {
    this.addressToWorkerMap = new ConcurrentHashMap<>();
    this.garbageCollector = new GarbageCollector(this, PRUNE_PERIOD);
  }

  public Optional<Worker<NodeInfo>> lookup(WorkerAddress address) {
    garbageCollector.tick();
    return Optional.ofNullable(addressToWorkerMap.get(address));
  }

  public Worker<NodeInfo> registerWorker(
      WorkerScript<NodeInfo> workerScript,
      WorkerAddress workerAddress,
      NodeInfo nodeInfo,
      PostalService postalService,
      Collection<Logger> loggers,
      Path crashDumpLocation,
      WorkerAddressGenerator workerAddressGenerator,
      WorkerAddressBook<NodeInfo> workerAddressBook) {
    Worker<NodeInfo> worker =
        new Worker<>(
            workerScript,
            workerAddress,
            nodeInfo,
            new Mailbox(),
            postalService,
            loggers,
            crashDumpLocation,
            workerAddressGenerator,
            workerAddressBook);
    addressToWorkerMap.put(workerAddress, worker);
    return worker;
  }

  @Override
  public void prune() {
    addressToWorkerMap.values().removeIf(worker -> worker.getState() == Worker.State.DEAD);
  }
}
