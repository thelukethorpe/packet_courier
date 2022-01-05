package thorpe.luke.network.simulator.worker;

public class WorkerException extends RuntimeException {
  public WorkerException(Exception e) {
    super(e);
  }

  public WorkerException(String message) {
    super(message);
  }
}
