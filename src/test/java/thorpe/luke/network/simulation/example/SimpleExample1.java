package thorpe.luke.network.simulation.example;

import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.PacketCourierSimulation;
import thorpe.luke.network.simulation.worker.WorkerManager;
import thorpe.luke.network.simulation.worker.WorkerTask;

public class SimpleExample1 {

  public static final String NODE_A_NAME = "Alice";
  public static final String NODE_B_NAME = "Bob";

  public static void runNodeA(WorkerManager workerManager) {
    // Node A sends the numbers 1 to 10 to node B.
    for (int i = 1; i <= 10; i++) {
      String message =
          "Hello "
              + workerManager.getTopology().getNodeAddress(NODE_B_NAME)
              + ", please print this message containing the number "
              + i
              + ".";
      Packet messageAsPacket = Packet.of(message);
      workerManager.sendMail(
          workerManager.getTopology().getNodeAddress(NODE_B_NAME).asRootWorkerAddress(),
          messageAsPacket);
    }
  }

  public static void runNodeB(WorkerManager workerManager) {
    // Node B waits for messages from node A and automatically shuts down after 5 seconds.
    WorkerTask.configure()
        .withTimeout(5, TimeUnit.SECONDS)
        .execute(
            () -> {
              do {
                Packet packet = workerManager.waitForMail();
                packet
                    .tryParse()
                    .ifPresent(
                        message ->
                            workerManager.log(
                                workerManager
                                        .getTopology()
                                        .getNodeAddress(NODE_B_NAME)
                                        .asRootWorkerAddress()
                                    + " has received the following message: "
                                    + message));
              } while (true);
            });
  }

  public static void main(String[] args) {
    // Packet pipeline drops half of the packets that travel through it.
    Random random = new Random();
    PacketCourierSimulation simulation =
        PacketCourierSimulation.configuration()
            .addNode(NODE_A_NAME, SimpleExample1::runNodeA)
            .addNode(NODE_B_NAME, SimpleExample1::runNodeB)
            .addConnection(
                NODE_A_NAME,
                NODE_B_NAME,
                PacketPipeline.parameters(
                    NetworkCondition.uniformPacketDrop(0.5, random),
                    NetworkCondition.uniformPacketLatency(
                        250.0, 1000.0, ChronoUnit.MILLIS, random)))
            .addLogger(ConsoleLogger.out())
            .usingWallClock()
            .configure();
    simulation.run();
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
