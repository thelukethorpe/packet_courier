package thorpe.luke.network.packet;

public class PacketException extends RuntimeException {
  public PacketException(Exception e) {
    super(e);
  }
}
