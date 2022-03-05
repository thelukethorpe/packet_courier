package thorpe.luke.network.simulation.example;

import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.NetworkEvent;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.DistributedNetworkSimulation;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.worker.WorkerManager;
import thorpe.luke.network.simulation.worker.WorkerTask;
import thorpe.luke.util.Mutable;

public class SimpleExample5 {

  public static final String NODE_A_NAME = "Alice";
  public static final String NODE_B_NAME = "Bob";

  public static void runNodeA(WorkerManager<SimpleExample1NodeInfo> workerManager) {
    // Node A sends the numbers 1 to 15,000 to node B.
    for (int i = 1; i <= 15_000; i++) {
      workerManager.sendMail(
          workerManager.getInfo().getNodeBAddress().asRootWorkerAddress(), Packet.of(i));
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void runNodeB(WorkerManager<SimpleExample1NodeInfo> workerManager) {
    // Node B waits for messages from node A and automatically shuts down after 15 seconds.
    WorkerTask.configure()
        .withTimeout(15, TimeUnit.SECONDS)
        .execute(
            () -> {
              Mutable<Integer> previous = new Mutable<>(0);
              do {
                Packet packet = workerManager.waitForMail();
                packet
                    .tryParse(Integer.class)
                    .ifPresent(
                        current -> {
                          if (current < previous.get()) {
                            workerManager.log("OUT OF ORDER: " + current);
                          } else if (previous.get() + 1 < current) {
                            workerManager.log("DROPPED DETECTED OR OUT OF ORDER: " + current);
                            previous.set(current);
                          } else {
                            workerManager.log(current.toString());
                            previous.set(current);
                          }
                        });

              } while (true);
            });
  }

  public static class SimpleExample1NodeInfo {
    public NodeAddress getNodeBAddress() {
      return new NodeAddress(NODE_B_NAME);
    }
  }

  public static void main(String[] args) {
    // Packet pipeline goes through phases of either dropping all packets or adding a uniformly distributed delay.
    Random random = new Random();
    DistributedNetworkSimulation<SimpleExample1NodeInfo> distributedNetworkSimulation =
        DistributedNetworkSimulation.configuration(
                (address, topology, clock) -> new SimpleExample1NodeInfo())
            .addNode(NODE_A_NAME, SimpleExample5::runNodeA)
            .addNode(NODE_B_NAME, SimpleExample5::runNodeB)
            .addConnection(
                NODE_A_NAME,
                NODE_B_NAME,
                PacketPipeline.parameters(
                    NetworkCondition.eventPipeline(
                        ChronoUnit.MILLIS,
                        random,
                        PacketPipeline.perfectParameters(),
                        NetworkEvent.builder()
                            .withMeanInterval(400.0)
                            .withMeanDuration(650.0)
                            .buildWithNetworkConditions(
                                NetworkCondition.uniformPacketDrop(1.0, random)),
                        NetworkEvent.builder()
                            .withMeanInterval(175.0)
                            .withMeanDuration(250.0)
                            .buildWithNetworkConditions(
                                NetworkCondition.uniformPacketLatency(
                                    35.0, 50.0, ChronoUnit.MILLIS, random)))))
            .addLogger(new ConsoleLogger())
            .usingWallClock()
            .start();
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
