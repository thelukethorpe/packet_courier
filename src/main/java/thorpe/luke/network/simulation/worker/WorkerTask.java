package thorpe.luke.network.simulation.worker;

import java.util.concurrent.*;

public class WorkerTask {
  private final Runnable runnable;
  private final long time;
  private final TimeUnit unit;

  public WorkerTask(Runnable runnable, long time, TimeUnit unit) {
    this.runnable = runnable;
    this.time = time;
    this.unit = unit;
  }

  private void execute() {
    Future<Void> result =
        CompletableFuture.supplyAsync(
            () -> {
              runnable.run();
              return null;
            });
    try {
      result.get(time, unit);
    } catch (ExecutionException e) {
      throw new WorkerException(e);
    } catch (InterruptedException | TimeoutException e) {
      // Do nothing.
    } finally {
      result.cancel(true);
    }
  }

  public static Configuration configure() {
    return new Configuration();
  }

  public static class Configuration {
    private long time = 0;
    private TimeUnit unit = TimeUnit.MILLISECONDS;

    private Configuration() {}

    public Configuration withTimeout(long time, TimeUnit unit) {
      this.time = time;
      this.unit = unit;
      return this;
    }

    public void execute(Runnable runnable) {
      WorkerTask workerTask = new WorkerTask(runnable, time, unit);
      workerTask.execute();
    }

    public WorkerScript asWorkerScript(WorkerScript workerScript) {
      return workerManager -> this.execute(() -> workerScript.run(workerManager));
    }
  }
}
