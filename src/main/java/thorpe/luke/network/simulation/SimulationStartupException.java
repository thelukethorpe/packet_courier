package thorpe.luke.network.simulation;

public class SimulationStartupException extends RuntimeException {
  public SimulationStartupException(String message) {
    super(message);
  }

  public SimulationStartupException(Exception e) {
    super(e);
  }
}
