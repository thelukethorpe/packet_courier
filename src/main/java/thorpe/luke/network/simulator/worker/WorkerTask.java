package thorpe.luke.network.simulator.worker;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.*;

public class WorkerTask {
  private final Runnable runnable;
  private final Duration duration;

  private WorkerTask(Runnable runnable, Duration duration) {
    this.runnable = runnable;
    this.duration = duration;
  }

  private void execute() {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<?> result = executorService.submit(runnable);
    try {
      if (duration == null) {
        result.get();
      } else {
        // Nanos are the smallest time unit for a Duration.
        result.get(duration.toNanos(), TimeUnit.NANOSECONDS);
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // Do nothing.
    } finally {
      // Kill any hanging threads.
      executorService.shutdownNow();
    }
  }

  public static Configuration configure() {
    return new Configuration();
  }

  public static class Configuration {
    private Duration duration;

    private Configuration() {}

    public Configuration withTimeout(long time, TemporalUnit unit) {
      duration = Duration.of(time, unit);
      return this;
    }

    public void execute(Runnable runnable) {
      WorkerTask workerTask = new WorkerTask(runnable, duration);
      workerTask.execute();
    }

    public <NodeInfo> WorkerScript<NodeInfo> asWorkerScript(WorkerScript<NodeInfo> workerScript) {
      return workerManager -> this.execute(() -> workerScript.run(workerManager));
    }
  }
}
