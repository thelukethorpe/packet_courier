package thorpe.luke.network.simulation.example;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.PacketCourierSimulation;
import thorpe.luke.network.simulation.worker.WorkerProcessConfiguration;

public class DatagramExample1 {

  public static final String NODE_A_NAME = "Alice";
  public static final String NODE_B_NAME = "Bob";
  public static final int PORT = 4444;
  public static final int DATAGRAM_BUFFER_SIZE = 32;
  public static final int NUMBER_OF_MESSAGES_SENT = 250;

  public static void main(String[] args) throws URISyntaxException {
    File simpleClientScript =
        Paths.get(ProtoExample1.class.getResource("simple_client.py").toURI()).toFile();
    File simpleServerScript =
        Paths.get(ProtoExample1.class.getResource("simple_server.py").toURI()).toFile();
    // Packet pipeline drops half of the packets that travel through it.
    Random random = new Random();
    PacketCourierSimulation simulation =
        PacketCourierSimulation.configuration()
            .addNode(
                NODE_A_NAME,
                WorkerProcessConfiguration.fromCommand(
                    String.format(
                        "python3 %s ${NODE_NAME} ${PRIVATE_IP} ${PORT} ${NEIGHBOUR_IPS} %s",
                        simpleClientScript.getAbsolutePath(), NUMBER_OF_MESSAGES_SENT)))
            .addNode(
                NODE_B_NAME,
                WorkerProcessConfiguration.fromCommand(
                    String.format(
                        "python3 %s ${NODE_NAME} ${PRIVATE_IP} ${PORT} ${DATAGRAM_BUFFER_SIZE}",
                        simpleServerScript.getAbsolutePath())))
            .addConnection(
                NODE_A_NAME,
                NODE_B_NAME,
                PacketPipeline.parameters(
                    NetworkCondition.uniformPacketDrop(0.5, random),
                    NetworkCondition.uniformPacketLatency(
                        250.0, 1000.0, ChronoUnit.MILLIS, random)))
            .addLogger(ConsoleLogger.out())
            .usingWallClock()
            .withPort(PORT)
            .withDatagramBufferSize(DATAGRAM_BUFFER_SIZE)
            .withCrashDumpLocation(Paths.get("."))
            .withProcessLoggingEnabled()
            .configure();
    simulation.run();
    System.out.println("Simulation complete. Exiting elegantly...");
  }
}
