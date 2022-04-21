package thorpe.luke.network.simulation.worker;

import java.io.IOException;

public class WorkerProcessFactory {
  private final ProcessBuilder processBuilder;

  public WorkerProcessFactory(ProcessBuilder processBuilder) {
    this.processBuilder = processBuilder;
  }

  public Process start() throws IOException {
    return processBuilder.start();
  }
}
