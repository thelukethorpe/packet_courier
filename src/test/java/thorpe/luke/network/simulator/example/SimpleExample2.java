package thorpe.luke.network.simulator.example;

import java.time.temporal.ChronoField;
import java.util.Random;
import java.util.concurrent.*;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.NetworkCondition.InvalidNetworkConditionException;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulator.DistributedNetworkSimulation;
import thorpe.luke.network.simulator.DistributedNetworkSimulation.InvalidSimulationConfigurationException;
import thorpe.luke.network.simulator.NodeManager;

public class SimpleExample2 {

  // Number of nodes.
  public static final int N = 5;

  public static void runNode(NodeManager nodeManager) {
    // Simple flood algorithm. Nodes timeout after 10 seconds.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Void> result =
        executorService.submit(
            () -> {
              for (String neighbour : nodeManager.getNeighbours()) {
                for (int i = 1; i <= N; i++) {
                  String message = nodeManager.getName() + "(" + i + ")";
                  Packet messageAsPacket = Packet.of(message);
                  nodeManager.sendMail(neighbour, messageAsPacket);
                }
              }

              do {
                Packet packet = nodeManager.waitForMail();
                String message = packet.asString();
                String newMessage =
                    message
                        + " -> "
                        + nodeManager.getName()
                        + ", received at t = "
                        + nodeManager.getCurrentTime().get(ChronoField.MILLI_OF_SECOND)
                        + "ms";
                Packet newMessageAsPacket = Packet.of(newMessage);
                System.out.println(newMessage);
                nodeManager
                    .getNeighbours()
                    .forEach(neighbour -> nodeManager.sendMail(neighbour, newMessageAsPacket));
              } while (true);
            });
    try {
      result.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // Do nothing.
    } finally {
      // Kill any hanging threads.
      executorService.shutdownNow();
    }
  }

  public static void main(String[] args) {
    // Nodes are connected in a simple ring topology and forward messages around the ring.
    DistributedNetworkSimulation distributedNetworkSimulation;
    try {
      Random random = new Random();
      PacketPipeline.Parameters lossyNetworkParameters =
          PacketPipeline.parameters(NetworkCondition.uniformPacketDrop(1.0 / N, random));

      DistributedNetworkSimulation.Configuration distributedNetworkSimulationConfiguration =
          DistributedNetworkSimulation.configuration();

      String[] nodeNames = new String[N];
      for (int i = 0; i < N; i++) {
        String nodeName = "Node " + (char) ('A' + i);
        nodeNames[i] = nodeName;
        distributedNetworkSimulationConfiguration.addNode(nodeName, SimpleExample2::runNode);
      }

      for (int i = 0; i < N; i++) {
        String currentNodeName = nodeNames[i];
        String nextNodeName = nodeNames[(i + 1) % N];
        distributedNetworkSimulationConfiguration.addConnection(
            currentNodeName, nextNodeName, lossyNetworkParameters);
      }

      distributedNetworkSimulation = distributedNetworkSimulationConfiguration.start();
    } catch (InvalidSimulationConfigurationException | InvalidNetworkConditionException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }
    try {
      distributedNetworkSimulation.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
