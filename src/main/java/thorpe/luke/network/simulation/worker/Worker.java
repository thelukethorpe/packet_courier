package thorpe.luke.network.simulation.worker;

import java.nio.file.Path;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.util.ExceptionListener;

public class Worker<NodeInfo> {

  private final Thread workerThread;
  private final WorkerAddress address;
  private final Mailbox mailbox;
  private State state;

  public Worker(
      WorkerScript<NodeInfo> workerScript,
      WorkerAddress address,
      NodeInfo nodeInfo,
      Mailbox mailbox,
      PostalService postalService,
      Logger logger,
      ExceptionListener exceptionListener,
      Path crashDumpLocation,
      WorkerAddressGenerator workerAddressGenerator,
      WorkerAddressBook<NodeInfo> workerAddressBook) {
    this.workerThread =
        new Thread(
            () ->
                workerScript.run(
                    new WorkerManager<>(
                        address,
                        nodeInfo,
                        mailbox,
                        postalService,
                        logger,
                        exceptionListener,
                        crashDumpLocation,
                        workerAddressGenerator,
                        workerAddressBook)));
    this.address = address;
    this.mailbox = mailbox;
    this.state = State.READY;
  }

  public WorkerAddress getAddress() {
    return address;
  }

  private synchronized void updateState(State oldState, State newState) {
    if (this.state != oldState) {
      throw new WorkerException(
          "Excepted \""
              + address.getName()
              + "\" to be in state "
              + oldState
              + ", but was in state "
              + this.state
              + " instead.");
    }
    this.state = newState;
  }

  public void run() {
    updateState(State.READY, State.RUNNING);
    workerThread.run();
    updateState(State.RUNNING, State.DEAD);
  }

  public void start() {
    updateState(State.READY, State.RUNNING);
    workerThread.start();
  }

  public void join() {
    try {
      workerThread.join();
      updateState(State.RUNNING, State.DEAD);
    } catch (InterruptedException e) {
      throw new WorkerException(e);
    }
  }

  public void kill() {
    workerThread.interrupt();
    updateState(State.RUNNING, State.DEAD);
  }

  public State getState() {
    return state;
  }

  public void post(Packet packet) {
    mailbox.post(packet);
  }

  public enum State {
    READY,
    RUNNING,
    DEAD
  }
}
