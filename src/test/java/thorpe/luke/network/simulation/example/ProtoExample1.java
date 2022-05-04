package thorpe.luke.network.simulation.example;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulation.PacketCourierSimulation;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.proto.PacketCourierSimulationConfigurationProtoParser;
import thorpe.luke.network.simulation.worker.WorkerManager;
import thorpe.luke.network.simulation.worker.WorkerScript;
import thorpe.luke.network.simulation.worker.WorkerTask;
import thorpe.luke.util.Mutable;

public class ProtoExample1 {

  public static final String NODE_A_NAME = "Alice";
  public static final String NODE_B_NAME = "Bob";

  public static void runNodeA(WorkerManager<ProtoExample1NodeInfo> workerManager) {
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

  public static void runNodeB(WorkerManager<ProtoExample1NodeInfo> workerManager) {
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

  public static class ProtoExample1NodeInfo {
    public NodeAddress getNodeBAddress() {
      return new NodeAddress(NODE_B_NAME);
    }
  }

  public static void main(String[] args) throws URISyntaxException {
    File configurationProtobufFile =
        Paths.get(ProtoExample1.class.getResource("example1.courierconfig").toURI()).toFile();
    PacketCourierSimulation<ProtoExample1NodeInfo> packetCourierSimulation =
        PacketCourierSimulationConfigurationProtoParser.parse(
                configurationProtobufFile,
                (address, topology, clock) -> new ProtoExample1NodeInfo(),
                new HashMap<String, WorkerScript<ProtoExample1NodeInfo>>() {
                  {
                    put(NODE_A_NAME, ProtoExample1::runNodeA);
                    put(NODE_B_NAME, ProtoExample1::runNodeB);
                  }
                })
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
