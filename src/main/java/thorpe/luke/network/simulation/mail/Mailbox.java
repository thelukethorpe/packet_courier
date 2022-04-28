package thorpe.luke.network.simulation.mail;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import thorpe.luke.network.packet.Packet;

public class Mailbox {

  private final BlockingQueue<Packet> packets;

  public Mailbox() {
    this.packets = new LinkedBlockingQueue<>();
  }

  public void post(Packet packet) {
    packets.offer(packet);
  }

  public Packet waitForMail() throws InterruptedException {
    return packets.take();
  }
}
