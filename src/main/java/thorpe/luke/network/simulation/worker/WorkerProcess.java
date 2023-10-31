package thorpe.luke.network.simulation.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import thorpe.luke.log.Logger;

public class WorkerProcess {

  private static final int SUCCESS_EXIT_CODE = 0;

  private final Process process;
  private final Duration timeout;

  private WorkerProcess(Process process, Duration timeout) {
    this.process = process;
    this.timeout = timeout;
  }

  public static Factory factoryOf(ProcessBuilder processBuilder, Duration timeout) {
    return new Factory(processBuilder, timeout);
  }

  public void logProcessOutput(Logger logger, int maxLogs) {
    InputStream inputStream = process.getInputStream();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    for (int i = 0; i < maxLogs; i++) {
      String processOutput;
      try {
        processOutput = bufferedReader.readLine();
      } catch (IOException e) {
        throw new WorkerException(e);
      }
      if (processOutput == null) {
        return;
      }
      logger.log(processOutput);
    }
  }

  public WorkerProcessExitStatus waitFor() throws InterruptedException, IOException {
    int exitStatus;
    boolean hasExited;
    if (timeout == null || timeout.isZero() || timeout.isNegative()) {
      exitStatus = process.waitFor();
      hasExited = true;
    } else {
      hasExited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
      exitStatus = hasExited ? process.exitValue() : SUCCESS_EXIT_CODE;
    }

    if (!hasExited) {
      process.destroy();
      process.waitFor();
    }

    if (exitStatus == SUCCESS_EXIT_CODE) {
      return WorkerProcessExitStatus.success();
    } else {
      return WorkerProcessExitStatus.failure(process.getErrorStream());
    }
  }

  public boolean isAlive() {
    return process.isAlive();
  }

  public static class Factory {
    private final ProcessBuilder processBuilder;
    private final Duration timeout;

    private Factory(ProcessBuilder processBuilder, Duration timeout) {
      this.processBuilder = processBuilder;
      this.timeout = timeout;
    }

    public WorkerProcess start() throws IOException {
      Process process = processBuilder.start();
      return new WorkerProcess(process, timeout);
    }
  }
}
