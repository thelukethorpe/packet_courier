package thorpe.luke.network.simulation.example;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.PacketCourierSimulation;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.worker.Worker;
import thorpe.luke.network.simulation.worker.WorkerAddress;
import thorpe.luke.network.simulation.worker.WorkerManager;

public class SimpleExample3 {

  public static final String NODE_A_NAME = "Alice";
  public static final String NODE_B_NAME = "Bob";
  // Number of workers on Node A.
  public static final int N = 5;

  public static void runNodeA(WorkerManager workerManager) {
    // Spawn child workers and flood their addresses to neighbours.
    Collection<Worker> workers =
        IntStream.range(0, N)
            .mapToObj(
                i ->
                    workerManager.spawnChildWorker(
                        childWorkerManager -> {
                          Collection<NodeAddress> neighbourAddresses =
                              workerManager
                                  .getTopology()
                                  .getNeighboursOf(
                                      childWorkerManager
                                          .getAddress()
                                          .getHostingNodeAddress()
                                          .getName());
                          Packet workerAddressPacket = Packet.of(childWorkerManager.getAddress());
                          for (NodeAddress neighbourAddress : neighbourAddresses) {
                            childWorkerManager.sendMail(
                                neighbourAddress.asRootWorkerAddress(), workerAddressPacket);
                            Packet responsePacket = childWorkerManager.waitForMail();
                            responsePacket
                                .tryParse(String.class)
                                .ifPresent(
                                    message ->
                                        childWorkerManager.log(
                                            childWorkerManager.getAddress()
                                                + " has received the following message: "
                                                + message));
                          }
                        }))
            .collect(Collectors.toList());
    workers.forEach(Worker::start);
    workers.forEach(Worker::join);
  }

  public static void runNodeB(WorkerManager workerManager) {
    // Node B waits for messages from node A and sends a simple response.
    for (int i = 1; i <= N; i++) {
      Packet packet = workerManager.waitForMail();
      WorkerAddress workerAddress = packet.tryParse(WorkerAddress.class).get();
      workerManager.sendMail(
          workerAddress,
          Packet.of(
              "Hello "
                  + workerAddress
                  + ", please print this message containing the number "
                  + i
                  + "."));
    }
  }

  public static void main(String[] args) {
    PacketCourierSimulation simulation =
        PacketCourierSimulation.configuration()
            .addNode(NODE_A_NAME, SimpleExample3::runNodeA)
            .addNode(NODE_B_NAME, SimpleExample3::runNodeB)
            .addConnection(NODE_A_NAME, NODE_B_NAME, PacketPipeline.perfectParameters())
            .addConnection(NODE_B_NAME, NODE_A_NAME, PacketPipeline.perfectParameters())
            .addLogger(ConsoleLogger.out())
            .configure();
    simulation.start();
    try {
      simulation.waitFor();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
