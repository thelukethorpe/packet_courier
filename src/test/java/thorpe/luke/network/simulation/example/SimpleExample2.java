package thorpe.luke.network.simulation.example;

import java.time.temporal.ChronoField;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.DistributedNetworkSimulation;
import thorpe.luke.network.simulation.node.DefaultNodeInfo;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.worker.WorkerManager;
import thorpe.luke.network.simulation.worker.WorkerTask;

public class SimpleExample2 {

  // Number of nodes.
  public static final int N = 5;

  public static void runNode(WorkerManager<DefaultNodeInfo> workerManager) {
    // Simple flood algorithm. Nodes timeout after 10 seconds.
    WorkerTask.configure()
        .withTimeout(10, TimeUnit.SECONDS)
        .execute(
            () -> {
              for (NodeAddress neighbour : workerManager.getInfo().getNeighbours()) {
                for (int i = 1; i <= N; i++) {
                  String message = workerManager.getAddress() + "[" + i + "]";
                  Packet messageAsPacket = Packet.of(message);
                  workerManager.sendMail(neighbour.asRootWorkerAddress(), messageAsPacket);
                }
              }

              do {
                Packet packet = workerManager.waitForMail();
                packet
                    .tryParse()
                    .ifPresent(
                        message -> {
                          String newMessage =
                              message
                                  + " -> "
                                  + workerManager.getAddress()
                                  + ", received at t = "
                                  + workerManager
                                      .getInfo()
                                      .getCurrentTime()
                                      .get(ChronoField.MILLI_OF_SECOND)
                                  + "ms";
                          Packet newMessageAsPacket = Packet.of(newMessage);
                          workerManager.log(newMessage);
                          workerManager
                              .getInfo()
                              .getNeighbours()
                              .forEach(
                                  neighbour ->
                                      workerManager.sendMail(
                                          neighbour.asRootWorkerAddress(), newMessageAsPacket));
                        });
              } while (true);
            });
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
      String nodeName = "#" + i;
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
      distributedNetworkSimulation.waitFor();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
