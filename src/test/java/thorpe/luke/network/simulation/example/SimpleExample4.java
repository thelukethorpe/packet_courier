package thorpe.luke.network.simulation.example;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.PacketCourierSimulation;
import thorpe.luke.network.simulation.worker.WorkerAddress;
import thorpe.luke.network.simulation.worker.WorkerManager;
import thorpe.luke.network.simulation.worker.WorkerTask;

public class SimpleExample4 {

  public static final String NODE_A_NAME = "Alice";
  public static final String NODE_B_NAME = "Bob";

  public static void runNodeA(WorkerManager workerManager) {
    WorkerAddress destinationAddress =
        workerManager.getTopology().getNodeAddress(NODE_B_NAME).asRootWorkerAddress();
    Stream.of("The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog.")
        .forEach(message -> workerManager.sendMail(destinationAddress, Packet.of(message)));
  }

  public static void runNodeB(WorkerManager workerManager) {
    // Node B waits for messages from node A and automatically shuts down after 5 seconds.
    WorkerTask.configure()
        .withTimeout(5, TimeUnit.SECONDS)
        .execute(
            () -> {
              do {
                Packet packet = workerManager.waitForMail();
                Optional<String> parsedPacket = packet.tryParse();
                if (parsedPacket.isPresent()) {
                  workerManager.log(
                      workerManager.getAddress()
                          + " has received the following message: "
                          + parsedPacket.get());
                } else {
                  workerManager.log(
                      workerManager.getAddress()
                          + " has received a packet that could not be parsed!");
                }
              } while (true);
            });
  }

  public static void main(String[] args) {
    // Packet pipeline corrupts 75% of the packets that travel through it.
    Random random = new Random();
    PacketCourierSimulation packetCourierSimulation =
        PacketCourierSimulation.configuration()
            .addNode(NODE_A_NAME, SimpleExample4::runNodeA)
            .addNode(NODE_B_NAME, SimpleExample4::runNodeB)
            .addConnection(
                NODE_A_NAME,
                NODE_B_NAME,
                PacketPipeline.parameters(NetworkCondition.uniformPacketCorruption(0.75, random)))
            .addLogger(ConsoleLogger.out())
            .configure();
    packetCourierSimulation.start();
    try {
      packetCourierSimulation.waitFor();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
