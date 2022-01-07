package thorpe.luke.network.simulation.worker;

public class WorkerException extends RuntimeException {
  public WorkerException(Exception e) {
    super(e);
  }

  public WorkerException(String message) {
    super(message);
  }
}
