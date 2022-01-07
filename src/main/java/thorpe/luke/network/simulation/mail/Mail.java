package thorpe.luke.network.simulation.mail;

import java.util.function.Function;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketWrapper;
import thorpe.luke.network.simulation.worker.WorkerAddress;

public class Mail implements PacketWrapper<Mail> {
  private final WorkerAddress destinationAddress;
  private final Packet packet;

  public Mail(WorkerAddress destinationAddress, Packet packet) {
    this.destinationAddress = destinationAddress;
    this.packet = packet;
  }

  public WorkerAddress getDestinationAddress() {
    return destinationAddress;
  }

  public Packet getPacket() {
    return packet;
  }

  @Override
  public Mail map(Function<Packet, Packet> function) {
    return new Mail(destinationAddress, function.apply(packet));
  }
}
