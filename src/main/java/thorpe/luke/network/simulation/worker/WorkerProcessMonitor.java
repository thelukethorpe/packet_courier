package thorpe.luke.network.simulation.worker;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import thorpe.luke.log.Logger;
import thorpe.luke.util.ThreadNameGenerator;

public class WorkerProcessMonitor {
  private final ConcurrentMap<String, WorkerProcess> nameToWorkerProcessMap =
      new ConcurrentHashMap<>();
  private final AtomicBoolean hasShutdown = new AtomicBoolean(false);
  private final Collection<Logger> loggers = new LinkedList<>();
  private final Duration checkupFrequency;
  private final Thread monitorThread;

  public WorkerProcessMonitor(Duration checkupFrequency, String name) {
    this.checkupFrequency = checkupFrequency;
    this.monitorThread = new Thread(this::run, ThreadNameGenerator.generateThreadName(name));
  }

  private void log(String message) {
    loggers.forEach(logger -> logger.log(message));
  }

  private void run() {
    while (!hasShutdown.get()) {
      log("Process monitor performing checkup");
      List<String> deadWorkerProcessNames = new LinkedList<>();
      for (Map.Entry<String, WorkerProcess> nameToWorkerProcessEntry :
          nameToWorkerProcessMap.entrySet()) {
        String workerProcessName = nameToWorkerProcessEntry.getKey();
        WorkerProcess workerProcess = nameToWorkerProcessEntry.getValue();
        if (workerProcess.isAlive()) {
          log("\"" + workerProcessName + "\" is alive");
        } else {
          log("\"" + workerProcessName + "\" has finished and is no longer being monitored");
          deadWorkerProcessNames.add(workerProcessName);
        }
      }
      deadWorkerProcessNames.forEach(nameToWorkerProcessMap::remove);
      try {
        Thread.sleep(checkupFrequency.toMillis());
      } catch (InterruptedException e) {

      }
    }
  }

  public void addLogger(Logger logger) {
    loggers.add(logger);
  }

  public void addProcess(String workerProcessName, WorkerProcess workerProcess) {
    nameToWorkerProcessMap.put(workerProcessName, workerProcess);
    log("\"" + workerProcessName + "\" is now up, running and being monitored");
  }

  public void start() {
    monitorThread.start();
  }

  public void shutdown() throws InterruptedException {
    log("Process monitor shutting down");
    hasShutdown.set(true);
    monitorThread.interrupt();
    monitorThread.join();
  }
}
