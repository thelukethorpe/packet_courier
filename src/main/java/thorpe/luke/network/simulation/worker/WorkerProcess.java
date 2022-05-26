package thorpe.luke.network.simulation.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import thorpe.luke.log.Logger;
import thorpe.luke.util.ThreadNameGenerator;

public class WorkerProcess {

  private static final int SUCCESS_EXIT_CODE = 0;

  private final String name;
  private final AtomicBoolean processComplete = new AtomicBoolean(false);
  private final Process process;
  private final Duration timeout;
  private final Logger logger;

  private WorkerProcess(String name, Process process, Duration timeout, Logger logger) {
    this.name = name;
    this.process = process;
    this.timeout = timeout;
    this.logger = logger;
  }

  public static Factory factoryOf(String name, ProcessBuilder processBuilder, Duration timeout) {
    return new Factory(name, processBuilder, timeout);
  }

  private void logProcessOutput() {
    if (logger == null) {
      return;
    }

    InputStream inputStream = process.getInputStream();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    while (!processComplete.get()) {
      String processOutput;
      try {
        processOutput = bufferedReader.readLine();
      } catch (IOException e) {
        throw new WorkerException(e);
      } catch (Throwable e) {
        return;
      }
      if (processOutput == null) {
        return;
      }
      logger.log(processOutput);
    }
  }

  public WorkerProcessExitStatus waitFor() throws InterruptedException, IOException {
    Thread processLoggingThread =
        new Thread(
            this::logProcessOutput, ThreadNameGenerator.generateThreadName(name + " Logger"));
    processLoggingThread.start();

    int exitStatus;
    boolean hasExited;
    if (timeout == null || timeout.isZero() || timeout.isNegative()) {
      exitStatus = process.waitFor();
      hasExited = true;
    } else {
      hasExited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
      exitStatus = hasExited ? process.exitValue() : SUCCESS_EXIT_CODE;
    }

    processComplete.set(true);
    if (!hasExited) {
      processLoggingThread.interrupt();
      process.destroy();
      process.waitFor();
    }

    processLoggingThread.join();

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
    private final String name;
    private final ProcessBuilder processBuilder;
    private final Duration timeout;

    private Factory(String name, ProcessBuilder processBuilder, Duration timeout) {
      this.name = name;
      this.processBuilder = processBuilder;
      this.timeout = timeout;
    }

    public WorkerProcess start() throws IOException {
      return start(null);
    }

    public WorkerProcess start(Logger logger) throws IOException {
      Process process = processBuilder.start();
      return new WorkerProcess(name, process, timeout, logger);
    }
  }
}
