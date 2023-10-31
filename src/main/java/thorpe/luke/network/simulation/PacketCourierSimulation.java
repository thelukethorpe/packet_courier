package thorpe.luke.network.simulation;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import thorpe.luke.log.Logger;
import thorpe.luke.log.MultiLogger;
import thorpe.luke.log.TemplateLogger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.packet.PacketPipeline.Parameters;
import thorpe.luke.network.simulation.mail.Mail;
import thorpe.luke.network.simulation.mail.PacketCourierPostalService;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.Node;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.node.NodeConnection;
import thorpe.luke.network.simulation.node.NodeTopology;
import thorpe.luke.network.simulation.worker.*;
import thorpe.luke.time.AsyncTickable;
import thorpe.luke.time.TickableClock;
import thorpe.luke.util.ThreadNameGenerator;
import thorpe.luke.network.socket.UniqueLoopbackIpv4AddressGenerator;

public class PacketCourierSimulation implements Simulation {

  public static final String LOG_FILE_EXTENSION = ".courierlog";
  public static final String CONFIGURATION_FILE_EXTENSION = ".courierconfig";
  public static final String CRASH_DUMP_FILE_EXTENSION = ".couriercrashdump";

  static final DateTimeFormatter LOG_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy-hh:mm:ss");

  static final DateTimeFormatter CRASH_DUMP_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy_MM_dd_hh_mm_ss");

  private final String simulationName;
  private final List<NodeSimulation> nodes;
  private final PacketCourierPostalService postalService;
  private final WorkerProcessMonitor workerProcessMonitor;
  private final int port;

  private PacketCourierSimulation(
      String simulationName,
      List<NodeSimulation> nodes,
      NodeTopology nodeTopology,
      PacketCourierPostalService postalService,
      boolean wallClockEnabled,
      TickableClock clock,
      Logger logger,
      WorkerProcessMonitor workerProcessMonitor,
      Path crashDumpLocation,
      int port,
      int datagramBufferSize,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<Node, DatagramSocket> nodeToPublicSocketMap) {
    this.simulationName = simulationName;
    this.nodes = nodes;
    this.postalService = postalService;
    this.logger =
        new TemplateLogger(
            message -> {
              LocalDateTime now = LocalDateTime.now();
              return String.format(
                  "%s-%s: %s.", LOG_DATE_FORMAT.format(now), simulationName, message);
            },
            logger);
  }

  public static Configuration configuration() {
    return new Configuration();
  }

  public String getName() {
    return simulationName;
  }

  private static void handleException(String name, Logger logger, Exception exception) {
    String stackTrace =
        Arrays.stream(exception.getStackTrace())
            .map(StackTraceElement::toString)
            .collect(Collectors.joining("\n"));
    logger.log(
        String.format(
            "%s encountered an exception: %s\n%s", name, exception.getMessage(), stackTrace));
  }

  private void listenForAndRouteDatagramPackets(
      int datagramBufferSize,
      DatagramSocket publicSocket,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<InetAddress, WorkerAddress> publicIpAddressToWorkerAddressMap,
      PostalService postalService) {
    byte[] buffer =
        new byte[datagramBufferSize + publicSocket.getLocalAddress().getAddress().length];
    do {
      DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
      try {
        publicSocket.setSoTimeout(5_000);
        publicSocket.receive(datagramPacket);
      } catch (SocketTimeoutException e) {
        if (hasFinished()) {
          return;
        }
      } catch (IOException e) {
        throw new PacketCourierSimulationDatagramRoutingException(e);
      }
      if (datagramPacket.getAddress() == null) {
        continue;
      }
      byte[] sourceIpAddress = datagramPacket.getAddress().getAddress();
      System.arraycopy(sourceIpAddress, 0, buffer, datagramBufferSize, sourceIpAddress.length);
      WorkerAddress sourceAddress =
          privateIpAddressToWorkerAddressMap.get(datagramPacket.getAddress());
      WorkerAddress destinationAddress =
          publicIpAddressToWorkerAddressMap.get(publicSocket.getLocalAddress());
      if (sourceAddress != null && destinationAddress != null) {
        postalService.mail(sourceAddress, destinationAddress, Packet.fromBytes(buffer));
      }
    } while (true);
  }

  @Override
  public boolean isComplete() {
    return nodes.stream().allMatch(NodeSimulation::isComplete);
  }

  @Override
  public void tick(LocalDateTime now) {
    nodes.forEach(node -> node.tick(now));
    postalService.tick(now);
  }

  private static class NodeSimulation implements Simulation {
    private final Node node;

    private NodeSimulation(Node node) {
      this.node = node;
    }

    @Override
    public boolean isComplete() {
      return !node.hasActiveWorkers();
    }

    @Override
    public void tick(LocalDateTime now) {
      node.tick(now);
    }
  }

  private static class AsyncNodeSimulation extends NodeSimulation {
    private final AsyncTickable asyncTicker;

    private AsyncNodeSimulation(Node node, ExecutorService executorService) {
      super(node);
      this.asyncTicker = new AsyncTickable(executorService, node);
    }

    @Override
    public void tick(LocalDateTime now) {
      asyncTicker.tick(now);
    }
  }

  @FunctionalInterface
  interface WorkerScriptFactory {

    WorkerScript getWorkerScript(
        NodeAddress address,
        NodeTopology nodeTopology,
        Path crashDumpLocation,
        Logger logger);
  }

  @FunctionalInterface
  private interface PacketPipelineFactory {
    PacketPipeline<Mail> getPacketPipeline(LocalDateTime startTime);
  }

  public static class Configuration {
    private final Map<String, Node> nameToNodeMap = new HashMap<>();
    private final Map<Node, WorkerScriptFactory> nodeToWorkerScriptFactoryMap = new HashMap<>();
    private final Map<NodeConnection, PacketPipelineFactory>
        nodeConnectionToPacketPipelineFactoryMap = new HashMap<>();
    private final Collection<Logger> loggers = new LinkedList<>();
    private String simulationName = "Packet Courier Simulation";
    private Path crashDumpLocation = null;
    private int port = 0;
    private int datagramBufferSize = 1024;
    private boolean processLoggingEnabled = false;
    private boolean processMonitorEnabled = false;
    private Duration processMonitorCheckupInterval = Duration.of(10, ChronoUnit.SECONDS);

    public Configuration() {}

    public String getSimulationName() {
      return simulationName;
    }

    private static boolean isBlank(String string) {
      return string.trim().isEmpty();
    }

    Node addNode(String name, WorkerScriptFactory workerScriptFactory) {
      if (name == null) {
        throw new PacketCourierSimulationConfigurationException("Node name cannot be null.");
      } else if (isBlank(name)) {
        throw new PacketCourierSimulationConfigurationException("Node name cannot be blank.");
      } else if (nameToNodeMap.containsKey(name)) {
        throw new PacketCourierSimulationConfigurationException(
            "Node with name \"" + name + "\" has already been added.");
      }

      Node node = new Node(name);
      nameToNodeMap.put(name, node);
      nodeToWorkerScriptFactoryMap.put(node, workerScriptFactory);
      NodeConnection nodeConnection = new NodeConnection(node, node);
      nodeConnectionToPacketPipelineFactoryMap.put(
          nodeConnection,
          startTime -> new PacketPipeline<>(PacketPipeline.perfectParameters(), startTime));
      return node;
    }

    public Configuration addNode(String name, WorkerScript workerScript) {
      if (workerScript == null) {
        throw new PacketCourierSimulationConfigurationException("Node script cannot be null.");
      }
      WorkerScriptFactory workerScriptFactory =
          (address,
              topology,
              crashDumpLocation,
              logger) -> workerScript;
      addNode(name, workerScriptFactory);
      return this;
    }



    public Configuration addConnection(
        String sourceName, String destinationName, Parameters packetPipelineParameters) {
      if (sourceName == null) {
        throw new PacketCourierSimulationConfigurationException("Source node name cannot be null.");
      } else if (isBlank(sourceName)) {
        throw new PacketCourierSimulationConfigurationException(
            "Source node name cannot be blank.");
      } else if (!nameToNodeMap.containsKey(sourceName)) {
        throw new PacketCourierSimulationConfigurationException(
            "Node with name \"" + sourceName + "\" has not been added yet.");
      } else if (destinationName == null) {
        throw new PacketCourierSimulationConfigurationException(
            "Destination node name cannot be null.");
      } else if (isBlank(destinationName)) {
        throw new PacketCourierSimulationConfigurationException(
            "Destination node name cannot be blank.");
      } else if (!nameToNodeMap.containsKey(destinationName)) {
        throw new PacketCourierSimulationConfigurationException(
            "Node with name \"" + destinationName + "\" has not been added yet.");
      } else if (sourceName.equals(destinationName)) {
        throw new PacketCourierSimulationConfigurationException(
            "A node cannot be connected to itself.");
      }

      Node sourceNode = nameToNodeMap.get(sourceName);
      Node destinationNode = nameToNodeMap.get(destinationName);
      NodeConnection nodeConnection = new NodeConnection(sourceNode, destinationNode);

      if (nodeConnectionToPacketPipelineFactoryMap.containsKey(nodeConnection)) {
        throw new PacketCourierSimulationConfigurationException(
            "Connection has already been added.");
      }

      nodeConnectionToPacketPipelineFactoryMap.put(
          nodeConnection, startTime -> new PacketPipeline<>(packetPipelineParameters, startTime));
      return this;
    }

    public Configuration withSimulationName(String simulationName) {
      this.simulationName = simulationName;
      return this;
    }

    public Configuration withCrashDumpLocation(Path crashDumpLocation) {
      this.crashDumpLocation = crashDumpLocation;
      return this;
    }

    public PacketCourierSimulation configure() {
      Logger logger = new MultiLogger(loggers);
      // Configure topology logic.
      NodeTopology.Builder nodeTopologyBuilder = NodeTopology.builder();
      nameToNodeMap.keySet().forEach(nodeTopologyBuilder::addNode);
      for (NodeConnection nodeConnection : nodeConnectionToPacketPipelineFactoryMap.keySet()) {
        String sourceName = nodeConnection.getSource().getAddress().getName();
        String destinationName = nodeConnection.getDestination().getAddress().getName();
        if (!sourceName.equals(destinationName)) {
          nodeTopologyBuilder.addConnection(sourceName, destinationName);
        }
      }
      NodeTopology nodeTopology = nodeTopologyBuilder.build();

      // Configure datagram socket logic.
      Map<Node, DatagramSocket> nodeToPublicSocketMap = new HashMap<>();
      if (this.hasDatagramRoutingLayer) {
        for (Node node : nameToNodeMap.values()) {
          try {
            nodeToPublicSocketMap.put(
                node,
                new DatagramSocket(port, uniqueIpAddressGenerator.generateUniqueIpv4Address()));
          } catch (SocketException e) {
            throw new PacketCourierSimulationConfigurationException(e);
          }
        }
      }
      Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap =
          nodeToPublicSocketMap
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      nodeToPublicSocketEntry ->
                          nodeToPublicSocketEntry.getKey().getAddress().asRootWorkerAddress(),
                      nodeToPublicSocketEntry ->
                          nodeToPublicSocketEntry.getValue().getLocalAddress()));

      // Configure worker script and private socket logic.
      WorkerProcessMonitor workerProcessMonitor =
          new WorkerProcessMonitor(
              processMonitorCheckupInterval, simulationName + " Process Monitor");
      Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap = new HashMap<>();
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap = new HashMap<>();

      // Configure packet pipeline logic.
      TickableClock clock =
          new TickableClock(
              wallClockEnabled
                  ? LocalDateTime.now()
                  : LocalDateTime.of(0, Month.JANUARY, 1, 0, 0, 0, 0));
      LocalDateTime startTime = clock.now();
      Map<NodeConnection, PacketPipeline<Mail>> nodeConnectionToPacketPipelineMap =
          nodeConnectionToPacketPipelineFactoryMap
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      nodeConnectionToPacketPipelineFactoryEntry ->
                          nodeConnectionToPacketPipelineFactoryEntry
                              .getValue()
                              .getPacketPipeline(startTime)));

      // Configure simulation logic.
      PacketCourierPostalService postalService =
          new PacketCourierPostalService(nameToNodeMap.values(), nodeConnectionToPacketPipelineMap);

      // Register workers.
      nameToNodeMap
              .values().forEach(
                      node -> {
                        InetAddress privateIpAddress =
                                uniqueIpAddressGenerator.generateUniqueIpv4Address();
                        DatagramSocket publicSocket = nodeToPublicSocketMap.get(node);
                        privateIpAddressToPublicSocketMap.put(privateIpAddress, publicSocket);
                        privateIpAddressToWorkerAddressMap.put(
                                privateIpAddress, node.getAddress().asRootWorkerAddress());
                        WorkerScriptFactory workerScriptFactory =
                                nodeToWorkerScriptFactoryMap.get(node);
                        WorkerScript workerScript =
                                workerScriptFactory.getWorkerScript(
                                        node.getAddress(),
                                        nodeTopology,
                                        port,
                                        privateIpAddress,
                                        workerAddressToPublicIpMap,
                                        datagramBufferSize,
                                        privateIpAddressToPublicSocketMap,
                                        processLoggingEnabled,
                                        workerProcessMonitor,
                                        crashDumpLocation,
                                        logger);
                        node.registerWorker(workerScript, nodeTopology, crashDumpLocation, postalService);
                      });

      return new PacketCourierSimulation(
          simulationName,
          new ArrayList<>(nameToNodeMap.values()),
          nodeTopology,
          postalService,
          crashDumpLocation,
          privateIpAddressToWorkerAddressMap,
          nodeToPublicSocketMap);
    }
  }
}
