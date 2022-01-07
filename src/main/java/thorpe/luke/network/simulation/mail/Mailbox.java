package thorpe.luke.network.simulation.mail;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulation.worker.WorkerException;

public class Mailbox {

  private final BlockingQueue<Packet> packets;

  public Mailbox() {
    this.packets = new LinkedBlockingQueue<>();
  }

  public void post(Packet packet) {
    packets.offer(packet);
  }

  public Packet waitForMail() {
    try {
      return packets.take();
    } catch (InterruptedException e) {
      throw new WorkerException(e);
    }
  }
}
