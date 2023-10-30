package thorpe.luke.network.simulation.worker;

import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.NodeTopology;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class WorkerAddressBook {
    private final Map<WorkerAddress, Worker> addressToWorkerMap = new HashMap<>();

    public Worker registerWorker(
            WorkerScript workerScript,
            WorkerAddress workerAddress,
            NodeTopology nodeTopology,
            Path crashDumpLocation,
            WorkerAddressGenerator workerAddressGenerator,
            PostalService postalService) {
        Worker worker =
                new Worker(
                        workerScript,
                        workerAddress,
                        nodeTopology,
                        crashDumpLocation,
                        workerAddressGenerator,
                        this,
                        new Mailbox(),
                        postalService);
        addressToWorkerMap.put(workerAddress, worker);
        return worker;
    }

    public boolean hasActiveWorkers() {
        return !addressToWorkerMap.isEmpty();
    }

    public Optional<Worker> lookup(WorkerAddress address) {
        return Optional.ofNullable(addressToWorkerMap.get(address));
    }

    public void forEachWorker(Consumer<Worker> action) {
        addressToWorkerMap.values().forEach(action);
    }

    public void removeDeadWorkers() {
        addressToWorkerMap.values().removeIf(worker -> !worker.isAlive());
    }
}
