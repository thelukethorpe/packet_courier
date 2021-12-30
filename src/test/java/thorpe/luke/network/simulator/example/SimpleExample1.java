package thorpe.luke.network.simulator.example;

import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.*;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.NetworkCondition.InvalidNetworkConditionException;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulator.DistributedNetworkSimulation;
import thorpe.luke.network.simulator.DistributedNetworkSimulation.InvalidSimulationConfigurationException;
import thorpe.luke.network.simulator.NodeManager;

public class SimpleExample1 {

  public static final String NODE_A_NAME = "Node A";
  public static final String NODE_B_NAME = "Node B";

  public static void runNodeA(NodeManager<SimpleExample1NodeInfo> nodeManager) {
    // Node A sends the numbers 1 to 10 to node B.
    for (int i = 1; i <= 10; i++) {
      String message =
          "Hello "
              + nodeManager.getInfo().getNodeBName()
              + ", please print this message containing the number "
              + i
              + ".";
      Packet messageAsPacket = Packet.of(message);
      nodeManager.sendMail(nodeManager.getInfo().getNodeBName(), messageAsPacket);
    }
  }

  public static void runNodeB(NodeManager<SimpleExample1NodeInfo> nodeManager) {
    // Node B waits for messages from node A and automatically shuts down after 5 seconds.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Void> result =
        executorService.submit(
            () -> {
              do {
                Packet packet = nodeManager.waitForMail();
                String message = packet.asString();
                nodeManager.log(
                    nodeManager.getInfo().getNodeBName()
                        + " has received the following message: "
                        + message);
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

  public static class SimpleExample1NodeInfo {
    public String getNodeBName() {
      return NODE_B_NAME;
    }
  }

  public static void main(String[] args) {
    // Packet pipeline drops half of the packets that travel through it.
    Random random = new Random();
    DistributedNetworkSimulation<SimpleExample1NodeInfo> distributedNetworkSimulation;
    try {
      distributedNetworkSimulation =
          DistributedNetworkSimulation.configuration(
                  (name, topology, clock) -> new SimpleExample1NodeInfo())
              .addNode(NODE_A_NAME, SimpleExample1::runNodeA)
              .addNode(NODE_B_NAME, SimpleExample1::runNodeB)
              .addConnection(
                  NODE_A_NAME,
                  NODE_B_NAME,
                  PacketPipeline.parameters(
                      NetworkCondition.uniformPacketDrop(0.5, random),
                      NetworkCondition.uniformPacketLatency(35.0, 50.0, ChronoUnit.MILLIS, random)))
              .addLogger(new ConsoleLogger())
              .start();
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
