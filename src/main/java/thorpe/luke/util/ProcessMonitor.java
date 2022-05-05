package thorpe.luke.util;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import thorpe.luke.log.Logger;

public class ProcessMonitor {
  private final ConcurrentMap<String, Process> nameToProcessMap = new ConcurrentHashMap<>();
  private final AtomicBoolean hasShutdown = new AtomicBoolean(false);
  private final Collection<Logger> loggers = new LinkedList<>();
  private final Duration checkupFrequency;
  private final Thread monitorThread;

  public ProcessMonitor(Duration checkupFrequency) {
    this.checkupFrequency = checkupFrequency;
    this.monitorThread = new Thread(this::run);
  }

  private void log(String message) {
    loggers.forEach(logger -> logger.log(message));
  }

  private void run() {
    while (!hasShutdown.get()) {
      log("Process monitor performing checkup");
      List<String> deadProcessNames = new LinkedList<>();
      for (Map.Entry<String, Process> nameToProcessEntry : nameToProcessMap.entrySet()) {
        String processName = nameToProcessEntry.getKey();
        Process process = nameToProcessEntry.getValue();
        if (process.isAlive()) {
          log("\"" + processName + "\" is alive");
        } else {
          log("\"" + processName + "\" has finished and is no longer being monitored");
          deadProcessNames.add(processName);
        }
      }
      deadProcessNames.forEach(nameToProcessMap::remove);
      try {
        Thread.sleep(checkupFrequency.toMillis());
      } catch (InterruptedException e) {

      }
    }
  }

  public void addLogger(Logger logger) {
    loggers.add(logger);
  }

  public void addProcess(String processName, Process process) {
    nameToProcessMap.put(processName, process);
    log("\"" + processName + "\" is now up, running and being monitored");
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
