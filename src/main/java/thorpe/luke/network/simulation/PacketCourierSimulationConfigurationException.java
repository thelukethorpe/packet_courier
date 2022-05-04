package thorpe.luke.network.simulation;

public class PacketCourierSimulationConfigurationException extends RuntimeException {
  public PacketCourierSimulationConfigurationException(String message) {
    super(message);
  }

  public PacketCourierSimulationConfigurationException(Exception e) {
    super(e);
  }
}
