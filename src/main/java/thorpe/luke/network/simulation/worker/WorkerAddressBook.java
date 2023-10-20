package thorpe.luke.network.simulation.worker;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import thorpe.luke.log.Logger;
import thorpe.luke.network.simulation.Topology;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.time.Clock;
import thorpe.luke.util.GarbageCollector;
import thorpe.luke.util.Prunable;
import thorpe.luke.util.error.ExceptionListener;

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
      Clock clock,
      Topology topology,
      ExceptionListener exceptionListener,
      Path crashDumpLocation,
      WorkerAddressGenerator workerAddressGenerator,
      WorkerAddressBook workerAddressBook,
      PostalService postalService,
      Logger logger) {
    Worker worker =
        new Worker(
            workerScript,
            workerAddress,
            clock,
            topology,
            logger,
            exceptionListener,
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
