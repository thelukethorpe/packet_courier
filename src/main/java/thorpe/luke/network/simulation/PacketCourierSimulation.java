package thorpe.luke.network.simulation;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import thorpe.luke.log.BufferedFileLogger;
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
import thorpe.luke.time.Clock;
import thorpe.luke.time.VirtualClock;
import thorpe.luke.time.WallClock;
import thorpe.luke.util.ThreadNameGenerator;
import thorpe.luke.util.UniqueLoopbackIpv4AddressGenerator;
import thorpe.luke.util.error.ExceptionListener;

public class PacketCourierSimulation {

  public static final String LOG_FILE_EXTENSION = ".courierlog";
  public static final String CONFIGURATION_FILE_EXTENSION = ".courierconfig";
  public static final String CRASH_DUMP_FILE_EXTENSION = ".couriercrashdump";

  private static final DateTimeFormatter LOG_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy-hh:mm:ss");

  private static final DateTimeFormatter CRASH_DUMP_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy_MM_dd_hh_mm_ss");

  private final String simulationName;
  private final PacketCourierPostalService postalService;
  private final Logger logger;
  private final WorkerProcessMonitor workerProcessMonitor;
  private final int port;
  private final Map<Node, Thread> nodeThreads;
  private final Collection<Thread> datagramRoutingThreads;

  private PacketCourierSimulation(
      String simulationName,
      Map<String, RunnableNode> nodes,
      NodeTopology nodeTopology,
      PacketCourierPostalService postalService,
      Clock clock,
      Logger logger,
      WorkerProcessMonitor workerProcessMonitor,
      Path crashDumpLocation,
      int port,
      int datagramBufferSize,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<Node, DatagramSocket> nodeToPublicSocketMap) {
    this.simulationName = simulationName;
    this.postalService = postalService;
    this.logger =
        new TemplateLogger(
            message -> {
              LocalDateTime now = LocalDateTime.now();
              return String.format(
                  "%s-%s: %s.", LOG_DATE_FORMAT.format(now), simulationName, message);
            },
            logger);
    this.workerProcessMonitor = workerProcessMonitor;
    this.port = port;
    this.logger.log("Preprocessing simulation");
    this.nodeThreads =
        nodes
            .values()
            .stream()
            .collect(
                Collectors.toMap(
                    RunnableNode::getNode,
                    runnableNode ->
                        new Thread(
                            () ->
                                runnableNode.run(
                                    nodeTopology, postalService, clock, crashDumpLocation),
                            ThreadNameGenerator.generateThreadName(
                                runnableNode.getNode().getAddress().getName()))));
    // Configure datagram routing layer logic.
    Map<InetAddress, WorkerAddress> publicIpAddressToWorkerAddressMap =
        nodeToPublicSocketMap
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    nodeToPublicSocketEntry -> nodeToPublicSocketEntry.getValue().getLocalAddress(),
                    nodeToPublicSocketEntry ->
                        nodeToPublicSocketEntry.getKey().getAddress().asRootWorkerAddress()));
    this.datagramRoutingThreads =
        nodeToPublicSocketMap
            .values()
            .stream()
            .map(
                publicSocket ->
                    new Thread(
                        () ->
                            listenForAndRouteDatagramPackets(
                                datagramBufferSize,
                                publicSocket,
                                privateIpAddressToWorkerAddressMap,
                                publicIpAddressToWorkerAddressMap,
                                postalService),
                        ThreadNameGenerator.generateThreadName(
                            "Datagram Router at " + publicSocket.getLocalAddress())))
            .collect(Collectors.toList());
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

  private void joinAll(Collection<Thread> threads) {
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        handleException(simulationName, logger, e);
      }
    }
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

  public void startWorkers() {
    if (workerProcessMonitor != null) {
      workerProcessMonitor.addLogger(logger);
      workerProcessMonitor.start();
    }
    if (!datagramRoutingThreads.isEmpty()) {
      datagramRoutingThreads.forEach(Thread::start);
      logger.log("Now listening on port " + port);
    }

    for (Map.Entry<Node, Thread> nodeThreadEntry : nodeThreads.entrySet()) {
      Node node = nodeThreadEntry.getKey();
      Thread nodeThread = nodeThreadEntry.getValue();
      nodeThread.start();
      logger.log("Node \"" + node.getAddress().getName() + "\" has been set to work");
    }

    logger.log("Simulation is now fully operational");
  }

  private void waitForWorkers() {
    logger.log("All nodes have completed their work; simulation is now cleaning up resources");
    joinAll(nodeThreads.values());
    joinAll(datagramRoutingThreads);
    if (workerProcessMonitor != null) {
      try {
        workerProcessMonitor.shutdown();
      } catch (InterruptedException e) {
        handleException(simulationName, logger, e);
      }
    }

    // Flush and close loggers.
    logger.log("Flushing and closing logger");
    logger.flush();
    logger.close();
  }

  private boolean hasFinished() {
    return nodeThreads.values().stream().noneMatch(Thread::isAlive);
  }

  private void tick(LocalDateTime now) {
    postalService.tick(now);
  }

  public void run() {
    startWorkers();
    while (!hasFinished()) {
      tick(LocalDateTime.now());
    }
    waitForWorkers();
  }

  private static class RunnableNode {
    private final Node node;
    private final WorkerScript workerScript;

    private RunnableNode(Node node, WorkerScript workerScript) {
      this.node = node;
      this.workerScript = workerScript;
    }

    public void run(
        NodeTopology nodeTopology,
        PostalService postalService,
        Clock clock,
        Path crashDumpLocation) {
      node.doWork(workerScript, clock, nodeTopology, crashDumpLocation, postalService);
    }

    public Node getNode() {
      return node;
    }
  }

  @FunctionalInterface
  private interface WorkerScriptFactory {

    WorkerScript getWorkerScript(
        NodeAddress address,
        NodeTopology nodeTopology,
        int port,
        InetAddress privateIpAddress,
        Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap,
        int datagramBufferSize,
        Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap,
        boolean processLoggingEnabled,
        WorkerProcessMonitor workerProcessMonitor,
        Path crashDumpLocation,
        Logger logger);
  }

  @FunctionalInterface
  private interface PacketPipelineFactory {
    PacketPipeline<Mail> getPacketPipeline(LocalDateTime startTime);
  }

  public static class Configuration {
    private final UniqueIpAddressGeneratorAdaptor uniqueIpAddressGenerator =
        new UniqueIpAddressGeneratorAdaptor();
    private final Map<String, Node> nameToNodeMap = new HashMap<>();
    private final Map<Node, WorkerScriptFactory> nodeToWorkerScriptFactoryMap = new HashMap<>();
    private final Map<NodeConnection, PacketPipelineFactory>
        nodeConnectionToPacketPipelineFactoryMap = new HashMap<>();
    private final Collection<Logger> loggers = new LinkedList<>();
    private String simulationName = "Packet Courier Simulation";
    private Clock clock = new VirtualClock(ChronoUnit.MILLIS);
    private boolean hasDatagramRoutingLayer = false;
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

    private void addNode(String name, WorkerScriptFactory workerScriptFactory) {
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
    }

    public Configuration addNode(String name, WorkerScript workerScript) {
      if (workerScript == null) {
        throw new PacketCourierSimulationConfigurationException("Node script cannot be null.");
      }
      WorkerScriptFactory workerScriptFactory =
          (address,
              topology,
              port,
              privateIpAddress,
              workerAddressToPublicIpMap,
              datagramBufferSize,
              privateIpAddressToPublicSocketMap,
              processLoggingEnabled,
              workerProcessMonitor,
              crashDumpLocation,
              logger) -> workerScript;
      addNode(name, workerScriptFactory);
      return this;
    }

    public Configuration addNode(
        String name, WorkerProcessConfiguration workerProcessConfiguration) {
      hasDatagramRoutingLayer = true;
      if (workerProcessConfiguration == null) {
        throw new PacketCourierSimulationConfigurationException(
            "Node process configuration cannot be null.");
      }
      WorkerScriptFactory workerScriptFactory =
          (address,
              topology,
              port,
              privateIpAddress,
              workerAddressToPublicIpMap,
              datagramBufferSize,
              privateIpAddressToPublicSocketMap,
              processLoggingEnabled,
              workerProcessMonitor,
              crashDumpLocation,
              logger) -> {
            WorkerProcess.Factory workerProcessFactory =
                workerProcessConfiguration.buildFactory(
                    address,
                    topology,
                    port,
                    privateIpAddress,
                    workerAddressToPublicIpMap,
                    datagramBufferSize);
            ExceptionListener exceptionListener =
                exception -> {
                  if (crashDumpLocation == null) {
                    return;
                  }
                  LocalDateTime now = LocalDateTime.now();
                  String crashDumpFileName =
                      address.getName().replaceAll("\\s+", "-")
                          + "__"
                          + CRASH_DUMP_DATE_FORMAT.format(now)
                          + PacketCourierSimulation.CRASH_DUMP_FILE_EXTENSION;
                  File crashDumpFile = crashDumpLocation.resolve(crashDumpFileName).toFile();
                  BufferedFileLogger crashDumpFileLogger;
                  try {
                    crashDumpFileLogger = new BufferedFileLogger(crashDumpFile);
                  } catch (IOException e) {
                    handleException(address.getName(), logger, e);
                    return;
                  }
                  handleException(address.getName(), crashDumpFileLogger, exception);
                  crashDumpFileLogger.flush();
                  crashDumpFileLogger.close();
                };
            return WorkerDatagramForwardingScript.builder()
                .withWorkerProcessFactory(workerProcessFactory)
                .withPort(port)
                .withPrivateIpAddress(privateIpAddress)
                .withDatagramBufferSize(datagramBufferSize)
                .withPrivateIpAddressToPublicSocketMap(privateIpAddressToPublicSocketMap)
                .withProcessLoggingEnabled(processLoggingEnabled)
                .withProcessMonitor(workerProcessMonitor)
                .withExceptionListener(exceptionListener)
                .withLogger(logger)
                .build();
          };
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

    public Configuration addLogger(Logger logger) {
      loggers.add(logger);
      return this;
    }

    public Configuration usingWallClock() {
      clock = new WallClock();
      return this;
    }

    public Configuration withCrashDumpLocation(Path crashDumpLocation) {
      this.crashDumpLocation = crashDumpLocation;
      return this;
    }

    public Configuration withPort(int port) {
      this.port = port;
      return this;
    }

    public Configuration withDatagramBufferSize(int datagramBufferSize) {
      this.datagramBufferSize = datagramBufferSize;
      return this;
    }

    public Configuration withProcessLoggingEnabled() {
      this.processLoggingEnabled = true;
      return this;
    }

    public Configuration withProcessMonitorEnabled() {
      this.processMonitorEnabled = true;
      return this;
    }

    public Configuration withProcessMonitorCheckupInterval(Duration processMonitorCheckupInterval) {
      this.processMonitorCheckupInterval = processMonitorCheckupInterval;
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
      Map<String, RunnableNode> nameToRunnableNodeMap =
          nameToNodeMap
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      nameToNodeEntry -> {
                        Node node = nameToNodeEntry.getValue();
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
                        return new RunnableNode(node, workerScript);
                      }));

      // Configure packet pipeline logic.
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

      return new PacketCourierSimulation(
          simulationName,
          nameToRunnableNodeMap,
          nodeTopology,
          postalService,
          clock,
          logger,
          processMonitorEnabled ? workerProcessMonitor : null,
          crashDumpLocation,
          port,
          datagramBufferSize,
          privateIpAddressToWorkerAddressMap,
          nodeToPublicSocketMap);
    }
  }

  private static class UniqueIpAddressGeneratorAdaptor {
    private final UniqueLoopbackIpv4AddressGenerator uniqueLoopbackIpv4AddressGenerator =
        new UniqueLoopbackIpv4AddressGenerator();

    public InetAddress generateUniqueIpv4Address() {
      try {
        InetAddress ipAddress = uniqueLoopbackIpv4AddressGenerator.generateUniqueIpv4Address();
        if (ipAddress == null) {
          throw new PacketCourierSimulationConfigurationException(
              "The Kernel has run out of fresh ip addresses.");
        }
        return ipAddress;
      } catch (UnknownHostException e) {
        throw new PacketCourierSimulationConfigurationException(e);
      }
    }
  }
}
