package thorpe.luke.network.packet;

public class InvalidNetworkConditionException extends RuntimeException {
  public InvalidNetworkConditionException(String message) {
    super(message);
  }
}
