package thorpe.luke.network.simulator.example;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.NetworkCondition.InvalidNetworkConditionException;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulator.DistributedNetworkSimulator;
import thorpe.luke.network.simulator.DistributedNetworkSimulator.InvalidSimulatorConfigurationException;
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
                  Packet messageAsPacket = new Packet(message.getBytes());
                  nodeManager.sendMail(neighbour, messageAsPacket);
                }
              }

              do {
                Packet packet = nodeManager.waitForMail();
                String message = new String(packet.getData());
                String newMessage = message + " -> " + nodeManager.getName();
                Packet newMessageAsPacket = new Packet(newMessage.getBytes());
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
    DistributedNetworkSimulator distributedNetworkSimulator;
    try {
      Random random = new Random();
      PacketPipeline.Parameters lossyNetworkParameters =
          PacketPipeline.parameters(NetworkCondition.uniformPacketDrop(1.0 / N, random));

      DistributedNetworkSimulator.Builder distributedNetworkSimulatorBuilder =
          DistributedNetworkSimulator.builder();

      String[] nodeNames = new String[N];
      for (int i = 0; i < N; i++) {
        String nodeName = "Node " + (char) ('A' + i);
        nodeNames[i] = nodeName;
        distributedNetworkSimulatorBuilder.addNode(nodeName, SimpleExample2::runNode);
      }

      for (int i = 0; i < N; i++) {
        String currentNodeName = nodeNames[i];
        String nextNodeName = nodeNames[(i + 1) % N];
        distributedNetworkSimulatorBuilder.addConnection(
            currentNodeName, nextNodeName, lossyNetworkParameters);
      }

      distributedNetworkSimulator = distributedNetworkSimulatorBuilder.build();
    } catch (InvalidSimulatorConfigurationException | InvalidNetworkConditionException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }
    distributedNetworkSimulator.start();
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
