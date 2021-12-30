package thorpe.luke.network.simulator.example;

import java.time.temporal.ChronoField;
import java.util.Random;
import java.util.concurrent.*;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulator.DefaultNodeInfo;
import thorpe.luke.network.simulator.DistributedNetworkSimulation;
import thorpe.luke.network.simulator.NodeManager;

public class SimpleExample2 {

  // Number of nodes.
  public static final int N = 5;

  public static void runNode(NodeManager<DefaultNodeInfo> nodeManager) {
    // Simple flood algorithm. Nodes timeout after 10 seconds.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Void> result =
        executorService.submit(
            () -> {
              for (String neighbour : nodeManager.getInfo().getNeighbours()) {
                for (int i = 1; i <= N; i++) {
                  String message = nodeManager.getInfo().getName() + "(" + i + ")";
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
                        + nodeManager.getInfo().getName()
                        + ", received at t = "
                        + nodeManager.getInfo().getCurrentTime().get(ChronoField.MILLI_OF_SECOND)
                        + "ms";
                Packet newMessageAsPacket = Packet.of(newMessage);
                nodeManager.log(newMessage);
                nodeManager
                    .getInfo()
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
    Random random = new Random();
    PacketPipeline.Parameters lossyNetworkParameters =
        PacketPipeline.parameters(NetworkCondition.uniformPacketDrop(1.0 / N, random));
    DistributedNetworkSimulation.Configuration<DefaultNodeInfo>
        distributedNetworkSimulationConfiguration =
            DistributedNetworkSimulation.configuration().addLogger(new ConsoleLogger());

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

    DistributedNetworkSimulation<DefaultNodeInfo> distributedNetworkSimulation =
        distributedNetworkSimulationConfiguration.start();
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
