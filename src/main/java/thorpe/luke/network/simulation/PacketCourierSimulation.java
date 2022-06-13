package thorpe.luke.network.simulation;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
import thorpe.luke.network.simulation.node.*;
import thorpe.luke.network.simulation.worker.*;
import thorpe.luke.network.simulation.worker.WorkerProcessMonitor;
import thorpe.luke.time.Clock;
import thorpe.luke.time.VirtualClock;
import thorpe.luke.time.WallClock;
import thorpe.luke.util.ExceptionListener;
import thorpe.luke.util.ThreadNameGenerator;
import thorpe.luke.util.UniqueLoopbackIpv4AddressGenerator;

public class PacketCourierSimulation<NodeInfo> {

  public static final String LOG_FILE_EXTENSION = ".courierlog";
  public static final String CONFIGURATION_FILE_EXTENSION = ".courierconfig";
  public static final String CRASH_DUMP_FILE_EXTENSION = ".couriercrashdump";

  private static final DateTimeFormatter META_LOG_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy-hh:mm:ss");

  private final String simulationName;
  private final Logger metaLogger;
  private final Thread simulationThread;

  private PacketCourierSimulation(
      String simulationName,
      Map<String, RunnableNode<NodeInfo>> nodes,
      Topology topology,
      PacketCourierPostalService<NodeInfo> postalService,
      Clock clock,
      Logger logger,
      Logger metaLogger,
      Optional<WorkerProcessMonitor> optionalProcessMonitor,
      Path crashDumpLocation,
      int port,
      int datagramBufferSize,
      int tickDurationSampleSize,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<Node<NodeInfo>, DatagramSocket> nodeToPublicSocketMap) {
    this.simulationName = simulationName;
    this.metaLogger =
        new TemplateLogger(
            message -> {
              LocalDateTime now = LocalDateTime.now();
              return String.format(
                  "%s-%s: %s.", META_LOG_DATE_FORMAT.format(now), simulationName, message);
            },
            metaLogger);
    this.simulationThread =
        new Thread(
            () ->
                this.run(
                    nodes,
                    topology,
                    postalService,
                    clock,
                    logger,
                    optionalProcessMonitor,
                    crashDumpLocation,
                    port,
                    datagramBufferSize,
                    tickDurationSampleSize,
                    privateIpAddressToWorkerAddressMap,
                    nodeToPublicSocketMap),
            ThreadNameGenerator.generateThreadName(simulationName));
  }

  public static <NodeInfo> Configuration<NodeInfo> configuration(
      NodeInfoGenerator<NodeInfo> nodeInfoGenerator) {
    return new Configuration<>(nodeInfoGenerator);
  }

  public static Configuration<DefaultNodeInfo> configuration() {
    return new Configuration<>(DefaultNodeInfo.generator());
  }

  public String getName() {
    return simulationName;
  }

  public void start() {
    try {
      simulationThread.start();
    } catch (IllegalThreadStateException e) {
      throw new PacketCourierSimulationStartupException(
          "Packet Courier Simulation has already started.");
    }
  }

  private void metaExceptionHandler(Exception exception) {
    String exceptionStackTrace =
        Arrays.stream(exception.getStackTrace())
            .map(StackTraceElement::toString)
            .collect(Collectors.joining("\n"));
    metaLogger.log("Internal simulation exception; stack trace:" + exceptionStackTrace);
  }

  private void run(
      Map<String, RunnableNode<NodeInfo>> nodes,
      Topology topology,
      PacketCourierPostalService<NodeInfo> postalService,
      Clock clock,
      Logger logger,
      Optional<WorkerProcessMonitor> optionalProcessMonitor,
      Path crashDumpLocation,
      int port,
      int datagramBufferSize,
      int tickDurationSampleSize,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<Node<NodeInfo>, DatagramSocket> nodeToPublicSocketMap) {
    // Configure simulation logic.
    metaLogger.log("Preprocessing simulation");
    AtomicBoolean simulationComplete = new AtomicBoolean(false);
    Map<Node<NodeInfo>, Thread> nodeThreads =
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
                                    topology,
                                    postalService,
                                    clock,
                                    logger,
                                    this::metaExceptionHandler,
                                    crashDumpLocation),
                            ThreadNameGenerator.generateThreadName(
                                runnableNode.getNode().getAddress().getName()))));
    Thread simulationManagerThread =
        new Thread(
            () -> {
              long total_tick_duration_in_nanoseconds = 0;
              int number_of_ticks_sampled = 0;
              while (!simulationComplete.get()) {
                clock.tick();
                long tick_start_in_nanoseconds = System.nanoTime();
                postalService.tick(clock.now());
                long tick_finish_in_nanoseconds = System.nanoTime();
                total_tick_duration_in_nanoseconds +=
                    tick_finish_in_nanoseconds - tick_start_in_nanoseconds;
                number_of_ticks_sampled++;
                if (number_of_ticks_sampled >= tickDurationSampleSize) {
                  double average_tick_duration_in_nanoseconds =
                      total_tick_duration_in_nanoseconds / (double) number_of_ticks_sampled;
                  metaLogger.log(
                      String.format(
                          "The last %d ticks have had an average duration of %.3f nanoseconds",
                          number_of_ticks_sampled, average_tick_duration_in_nanoseconds));
                  total_tick_duration_in_nanoseconds = 0;
                  number_of_ticks_sampled = 0;
                }
              }
            },
            ThreadNameGenerator.generateThreadName(simulationName + " Manager"));

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
    Collection<Thread> datagramRoutingThreads =
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
                                postalService,
                                simulationComplete),
                        ThreadNameGenerator.generateThreadName(
                            "Datagram Router at " + publicSocket.getLocalAddress())))
            .collect(Collectors.toList());

    // Start threads.
    optionalProcessMonitor.ifPresent(
        workerProcessMonitor -> {
          workerProcessMonitor.addLogger(metaLogger);
          workerProcessMonitor.start();
        });
    if (!datagramRoutingThreads.isEmpty()) {
      datagramRoutingThreads.forEach(Thread::start);
      metaLogger.log("Now listening on port " + port);
    }

    for (Map.Entry<Node<NodeInfo>, Thread> nodeThreadEntry : nodeThreads.entrySet()) {
      Node<NodeInfo> node = nodeThreadEntry.getKey();
      Thread nodeThread = nodeThreadEntry.getValue();
      nodeThread.start();
      metaLogger.log("Node \"" + node.getAddress().getName() + "\" has been set to work");
    }

    simulationManagerThread.start();
    metaLogger.log("Simulation is now fully operational");

    // Wait for threads.
    joinAll(nodeThreads.values());
    metaLogger.log("All nodes have completed their work; simulation is now cleaning up resources");
    simulationComplete.set(true);
    joinAll(datagramRoutingThreads);
    optionalProcessMonitor.ifPresent(
        workerProcessMonitor -> {
          try {
            workerProcessMonitor.shutdown();
          } catch (InterruptedException e) {
            metaExceptionHandler(e);
          }
        });

    try {
      simulationManagerThread.join();
    } catch (InterruptedException e) {
      metaExceptionHandler(e);
    }

    metaLogger.log("Simulation complete");

    // Flush and close loggers.
    metaLogger.log("Flushing and closing loggers");
    logger.flush();
    logger.close();
    metaLogger.flush();
    metaLogger.close();
  }

  private void joinAll(Collection<Thread> threads) {
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        metaExceptionHandler(e);
      }
    }
  }

  private static void listenForAndRouteDatagramPackets(
      int datagramBufferSize,
      DatagramSocket publicSocket,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<InetAddress, WorkerAddress> publicIpAddressToWorkerAddressMap,
      PostalService postalService,
      AtomicBoolean simulationComplete) {
    byte[] buffer =
        new byte[datagramBufferSize + publicSocket.getLocalAddress().getAddress().length];
    do {
      DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
      try {
        publicSocket.setSoTimeout(5_000);
        publicSocket.receive(datagramPacket);
      } catch (SocketTimeoutException e) {
        if (simulationComplete.get()) {
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

  public void waitFor() throws InterruptedException {
    simulationThread.join();
  }

  private static class RunnableNode<NodeInfo> {
    private final Node<NodeInfo> node;
    private final WorkerScript<NodeInfo> workerScript;
    private final NodeInfoGenerator<NodeInfo> nodeInfoGenerator;

    private RunnableNode(
        Node<NodeInfo> node,
        WorkerScript<NodeInfo> workerScript,
        NodeInfoGenerator<NodeInfo> nodeInfoGenerator) {
      this.node = node;
      this.workerScript = workerScript;
      this.nodeInfoGenerator = nodeInfoGenerator;
    }

    public void run(
        Topology topology,
        PostalService postalService,
        Clock clock,
        Logger logger,
        ExceptionListener exceptionListener,
        Path crashDumpLocation) {
      node.doWork(
          workerScript,
          nodeInfoGenerator.generateInfo(node.getAddress(), topology, clock),
          postalService,
          logger,
          exceptionListener,
          crashDumpLocation);
    }

    public Node<NodeInfo> getNode() {
      return node;
    }
  }

  @FunctionalInterface
  private interface WorkerScriptFactory<NodeInfo> {

    WorkerScript<NodeInfo> getWorkerScript(
        NodeAddress address,
        Topology topology,
        int port,
        InetAddress privateIpAddress,
        Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap,
        int datagramBufferSize,
        Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap,
        boolean processLoggingEnabled,
        WorkerProcessMonitor workerProcessMonitor);
  }

  @FunctionalInterface
  private interface PacketPipelineFactory {
    PacketPipeline<Mail> getPacketPipeline(LocalDateTime startTime);
  }

  public static class Configuration<NodeInfo> {
    private final UniqueIpAddressGeneratorAdaptor uniqueIpAddressGenerator =
        new UniqueIpAddressGeneratorAdaptor();
    private final Map<String, Node<NodeInfo>> nameToNodeMap = new HashMap<>();
    private final Map<Node<NodeInfo>, WorkerScriptFactory<NodeInfo>> nodeToWorkerScriptFactoryMap =
        new HashMap<>();
    private final Map<NodeConnection<NodeInfo>, PacketPipelineFactory>
        nodeConnectionToPacketPipelineFactoryMap = new HashMap<>();
    private final Collection<Logger> loggers = new LinkedList<>();
    private final Collection<Logger> metaLoggers = new LinkedList<>();
    private final NodeInfoGenerator<NodeInfo> nodeInfoGenerator;
    private String simulationName = "Packet Courier Simulation";
    private Clock clock = new VirtualClock(ChronoUnit.MILLIS);
    private boolean hasDatagramRoutingLayer = false;
    private Path crashDumpLocation = null;
    private int port = 0;
    private int datagramBufferSize = 1024;
    private boolean processLoggingEnabled = false;
    private boolean processMonitorEnabled = false;
    private Duration processMonitorCheckupInterval = Duration.of(10, ChronoUnit.SECONDS);
    private int tickDurationSampleSize = 1_000_000;

    public Configuration(NodeInfoGenerator<NodeInfo> nodeInfoGenerator) {
      this.nodeInfoGenerator = nodeInfoGenerator;
    }

    public String getSimulationName() {
      return simulationName;
    }

    private static boolean isBlank(String string) {
      return string.trim().isEmpty();
    }

    private void addNode(String name, WorkerScriptFactory<NodeInfo> workerScriptFactory) {
      if (name == null) {
        throw new PacketCourierSimulationConfigurationException("Node name cannot be null.");
      } else if (isBlank(name)) {
        throw new PacketCourierSimulationConfigurationException("Node name cannot be blank.");
      } else if (nameToNodeMap.containsKey(name)) {
        throw new PacketCourierSimulationConfigurationException(
            "Node with name \"" + name + "\" has already been added.");
      }

      Node<NodeInfo> node = new Node<>(name);
      nameToNodeMap.put(name, node);
      nodeToWorkerScriptFactoryMap.put(node, workerScriptFactory);
      NodeConnection<NodeInfo> nodeConnection = new NodeConnection<>(node, node);
      nodeConnectionToPacketPipelineFactoryMap.put(
          nodeConnection,
          startTime -> new PacketPipeline<>(PacketPipeline.perfectParameters(), startTime));
    }

    public Configuration<NodeInfo> addNode(String name, WorkerScript<NodeInfo> workerScript) {
      if (workerScript == null) {
        throw new PacketCourierSimulationConfigurationException("Node script cannot be null.");
      }
      addNode(
          name,
          (address,
              topology,
              port,
              privateIpAddress,
              workerAddressToPublicIpMap,
              datagramBufferSize,
              privateIpAddressToPublicSocketMap,
              processLoggingEnabled,
              workerProcessMonitor) -> workerScript);
      return this;
    }

    public Configuration<NodeInfo> addNode(
        String name, WorkerProcessConfiguration workerProcessConfiguration) {
      hasDatagramRoutingLayer = true;
      if (workerProcessConfiguration == null) {
        throw new PacketCourierSimulationConfigurationException(
            "Node process configuration cannot be null.");
      }
      addNode(
          name,
          (address,
              topology,
              port,
              privateIpAddress,
              workerAddressToPublicIpMap,
              datagramBufferSize,
              privateIpAddressToPublicSocketMap,
              processLoggingEnabled,
              workerProcessMonitor) -> {
            WorkerProcess.Factory workerProcessFactory =
                workerProcessConfiguration.buildFactory(
                    address,
                    topology,
                    port,
                    privateIpAddress,
                    workerAddressToPublicIpMap,
                    datagramBufferSize);
            return WorkerDatagramForwardingScript.builder()
                .withWorkerProcessFactory(workerProcessFactory)
                .withPort(port)
                .withPrivateIpAddress(privateIpAddress)
                .withDatagramBufferSize(datagramBufferSize)
                .withPrivateIpAddressToPublicSocketMap(privateIpAddressToPublicSocketMap)
                .withProcessLoggingEnabled(processLoggingEnabled)
                .withProcessMonitor(workerProcessMonitor)
                .build();
          });
      return this;
    }

    public Configuration<NodeInfo> addConnection(
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

      Node<NodeInfo> sourceNode = nameToNodeMap.get(sourceName);
      Node<NodeInfo> destinationNode = nameToNodeMap.get(destinationName);
      NodeConnection<NodeInfo> nodeConnection = new NodeConnection<>(sourceNode, destinationNode);

      if (nodeConnectionToPacketPipelineFactoryMap.containsKey(nodeConnection)) {
        throw new PacketCourierSimulationConfigurationException(
            "Connection has already been added.");
      }

      nodeConnectionToPacketPipelineFactoryMap.put(
          nodeConnection, startTime -> new PacketPipeline<>(packetPipelineParameters, startTime));
      return this;
    }

    public Configuration<NodeInfo> withSimulationName(String simulationName) {
      this.simulationName = simulationName;
      return this;
    }

    public Configuration<NodeInfo> addLogger(Logger logger) {
      loggers.add(logger);
      return this;
    }

    public Configuration<NodeInfo> addMetaLogger(Logger metaLogger) {
      metaLoggers.add(metaLogger);
      return this;
    }

    public Configuration<NodeInfo> usingWallClock() {
      clock = new WallClock();
      return this;
    }

    public Configuration<NodeInfo> withCrashDumpLocation(Path crashDumpLocation) {
      this.crashDumpLocation = crashDumpLocation;
      return this;
    }

    public Configuration<NodeInfo> withPort(int port) {
      this.port = port;
      return this;
    }

    public Configuration<NodeInfo> withDatagramBufferSize(int datagramBufferSize) {
      this.datagramBufferSize = datagramBufferSize;
      return this;
    }

    public Configuration<NodeInfo> withProcessLoggingEnabled() {
      this.processLoggingEnabled = true;
      return this;
    }

    public Configuration<NodeInfo> withProcessMonitorEnabled() {
      this.processMonitorEnabled = true;
      return this;
    }

    public Configuration<NodeInfo> withProcessMonitorCheckupInterval(
        Duration processMonitorCheckupInterval) {
      this.processMonitorCheckupInterval = processMonitorCheckupInterval;
      return this;
    }

    public Configuration<NodeInfo> withTickDurationSampleSize(int tickDurationSampleSize) {
      this.tickDurationSampleSize = tickDurationSampleSize;
      return this;
    }

    public PacketCourierSimulation<NodeInfo> configure() {
      // Configure topology logic.
      Map<String, Collection<String>> nodeToNeighboursMap = new HashMap<>();
      nameToNodeMap.keySet().forEach(name -> nodeToNeighboursMap.put(name, new HashSet<>()));
      for (NodeConnection<NodeInfo> nodeConnection :
          nodeConnectionToPacketPipelineFactoryMap.keySet()) {
        String sourceName = nodeConnection.getSource().getAddress().getName();
        String destinationName = nodeConnection.getDestination().getAddress().getName();
        if (!sourceName.equals(destinationName)) {
          nodeToNeighboursMap.get(sourceName).add(destinationName);
        }
      }
      Topology topology = Topology.of(nodeToNeighboursMap);

      // Configure datagram socket logic.
      Map<Node<NodeInfo>, DatagramSocket> nodeToPublicSocketMap = new HashMap<>();
      if (this.hasDatagramRoutingLayer) {
        for (Node<NodeInfo> node : nameToNodeMap.values()) {
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
      Map<String, RunnableNode<NodeInfo>> nameToRunnableNodeMap =
          nameToNodeMap
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      nameToNodeEntry -> {
                        Node<NodeInfo> node = nameToNodeEntry.getValue();
                        InetAddress privateIpAddress =
                            uniqueIpAddressGenerator.generateUniqueIpv4Address();
                        DatagramSocket publicSocket = nodeToPublicSocketMap.get(node);
                        privateIpAddressToPublicSocketMap.put(privateIpAddress, publicSocket);
                        privateIpAddressToWorkerAddressMap.put(
                            privateIpAddress, node.getAddress().asRootWorkerAddress());
                        WorkerScriptFactory<NodeInfo> workerScriptFactory =
                            nodeToWorkerScriptFactoryMap.get(node);
                        WorkerScript<NodeInfo> workerScript =
                            workerScriptFactory.getWorkerScript(
                                node.getAddress(),
                                topology,
                                port,
                                privateIpAddress,
                                workerAddressToPublicIpMap,
                                datagramBufferSize,
                                privateIpAddressToPublicSocketMap,
                                processLoggingEnabled,
                                workerProcessMonitor);
                        return new RunnableNode<>(node, workerScript, nodeInfoGenerator);
                      }));

      // Configure packet pipeline logic.
      LocalDateTime startTime = clock.now();
      Map<NodeConnection<NodeInfo>, PacketPipeline<Mail>> nodeConnectionToPacketPipelineMap =
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
      PacketCourierPostalService<NodeInfo> postalService =
          new PacketCourierPostalService<>(
              nameToNodeMap.values(), nodeConnectionToPacketPipelineMap);

      return new PacketCourierSimulation<>(
          simulationName,
          nameToRunnableNodeMap,
          topology,
          postalService,
          clock,
          new MultiLogger(loggers),
          new MultiLogger(metaLoggers),
          processMonitorEnabled ? Optional.of(workerProcessMonitor) : Optional.empty(),
          crashDumpLocation,
          port,
          datagramBufferSize,
          tickDurationSampleSize,
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
