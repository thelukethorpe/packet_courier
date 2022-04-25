package thorpe.luke.network.simulation;

public class PacketCourierSimulationStartupException extends RuntimeException {
  public PacketCourierSimulationStartupException(String message) {
    super(message);
  }

  public PacketCourierSimulationStartupException(Exception e) {
    super(e);
  }
}
