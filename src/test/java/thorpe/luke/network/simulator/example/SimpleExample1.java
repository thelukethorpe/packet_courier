package thorpe.luke.network.simulator.example;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.packet.SimulatedDropPacketFilter;
import thorpe.luke.network.simulator.DistributedNetworkSimulator;
import thorpe.luke.network.simulator.DistributedNetworkSimulator.InvalidSimulatorConfigurationException;
import thorpe.luke.network.simulator.NodeManager;

public class SimpleExample1 {

  public static final String NODE_A_NAME = "Node A";
  public static final String NODE_B_NAME = "Node B";

  public static void runNodeA(NodeManager nodeManager) {
    // Node A sends the numbers 1 to 10 to node B.
    for (int i = 1; i <= 10; i++) {
      String message =
          "Hello " + NODE_B_NAME + ", please print this message containing the number " + i + ".";
      Packet messageAsPacket = new Packet(message.getBytes());
      nodeManager.sendMail(NODE_B_NAME, messageAsPacket);
    }
  }

  public static void runNodeB(NodeManager nodeManager) {
    // Node B waits for messages from node A and automatically shuts down after 5 seconds.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Void> result =
        executorService.submit(
            () -> {
              do {
                Packet packet = nodeManager.waitForMail();
                String message = new String(packet.getData());
                System.out.println("Node B has received the following message: " + message);
              } while (true);
            });
    try {
      result.get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // Do nothing.
    } finally {
      // Kill any hanging threads.
      executorService.shutdownNow();
    }
  }

  public static void main(String[] args) {
    // Packet pipeline drops half of the packets that travel through it.
    PacketPipeline packetPipeline = new PacketPipeline(new SimulatedDropPacketFilter(0.5));
    DistributedNetworkSimulator distributedNetworkSimulator;
    try {
      distributedNetworkSimulator =
          DistributedNetworkSimulator.builder()
              .addNode(NODE_A_NAME, SimpleExample1::runNodeA)
              .addNode(NODE_B_NAME, SimpleExample1::runNodeB)
              .addConnection(NODE_A_NAME, NODE_B_NAME, packetPipeline)
              .build();
    } catch (InvalidSimulatorConfigurationException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }
    distributedNetworkSimulator.start();
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
