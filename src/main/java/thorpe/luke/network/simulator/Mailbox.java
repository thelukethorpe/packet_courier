package thorpe.luke.network.simulator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import thorpe.luke.network.packet.Packet;

public class Mailbox {

  private final BlockingQueue<Packet> messages;

  public Mailbox() {
    this.messages = new LinkedBlockingQueue<>();
  }

  public void post(Packet packet) {
    messages.offer(packet);
  }

  public Packet waitForMail() throws InterruptedException {
    return messages.take();
  }
}
