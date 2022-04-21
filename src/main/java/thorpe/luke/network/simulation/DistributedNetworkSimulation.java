package thorpe.luke.network.simulation;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.packet.PacketPipeline.Parameters;
import thorpe.luke.network.simulation.mail.Mail;
import thorpe.luke.network.simulation.mail.NetworkSimulatorPostalService;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.*;
import thorpe.luke.network.simulation.worker.*;
import thorpe.luke.time.Clock;
import thorpe.luke.time.VirtualClock;
import thorpe.luke.time.WallClock;
import thorpe.luke.util.UniqueLoopbackIpv4AddressGenerator;

public class DistributedNetworkSimulation<NodeInfo> {

  private final Thread simulator;

  private DistributedNetworkSimulation(
      Map<String, RunnableNode<NodeInfo>> nodes,
      Topology topology,
      NetworkSimulatorPostalService<NodeInfo> postalService,
      Clock clock,
      Collection<Logger> loggers,
      Path crashDumpLocation,
      int datagramBufferSize,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<Node<NodeInfo>, DatagramSocket> nodeToPublicSocketMap) {
    this.simulator =
        new Thread(
            () ->
                this.run(
                    nodes,
                    topology,
                    postalService,
                    clock,
                    loggers,
                    crashDumpLocation,
                    datagramBufferSize,
                    privateIpAddressToWorkerAddressMap,
                    nodeToPublicSocketMap));
  }

  public static <NodeInfo> Configuration<NodeInfo> configuration(
      NodeInfoGenerator<NodeInfo> nodeInfoGenerator) {
    return new Configuration<>(nodeInfoGenerator);
  }

  public static Configuration<DefaultNodeInfo> configuration() {
    return new Configuration<>(DefaultNodeInfo.generator());
  }

  private void start() {
    simulator.start();
  }

  private void run(
      Map<String, RunnableNode<NodeInfo>> nodes,
      Topology topology,
      NetworkSimulatorPostalService<NodeInfo> postalService,
      Clock clock,
      Collection<Logger> loggers,
      Path crashDumpLocation,
      int datagramBufferSize,
      Map<InetAddress, WorkerAddress> privateIpAddressToWorkerAddressMap,
      Map<Node<NodeInfo>, DatagramSocket> nodeToPublicSocketMap) {
    // Configure simulation logic.
    AtomicBoolean simulationComplete = new AtomicBoolean(false);
    Collection<Thread> nodeThreads =
        nodes
            .values()
            .stream()
            .map(
                runnableNode ->
                    new Thread(
                        () ->
                            runnableNode.run(
                                topology, postalService, clock, loggers, crashDumpLocation)))
            .collect(Collectors.toList());
    Thread simulationManagerThread =
        new Thread(
            () -> {
              while (!simulationComplete.get()) {
                clock.tick();
                postalService.tick(clock.now());
              }
            });

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
                                simulationComplete)))
            .collect(Collectors.toList());

    // Start threads.
    datagramRoutingThreads.forEach(Thread::start);
    nodeThreads.forEach(Thread::start);
    simulationManagerThread.start();

    // Wait for threads.
    joinAll(nodeThreads);
    simulationComplete.set(true);
    joinAll(datagramRoutingThreads);

    try {
      simulationManagerThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void joinAll(Collection<Thread> threads) {
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
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
        throw new SimulationDatagramRoutingException(e);
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
    simulator.join();
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
        Collection<Logger> loggers,
        Path crashDumpLocation) {
      node.doWork(
          workerScript,
          nodeInfoGenerator.generateInfo(node.getAddress(), topology, clock),
          postalService,
          loggers,
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
        boolean processLoggingEnabled);
  }

  @FunctionalInterface
  private interface PacketPipelineFactory {
    PacketPipeline<Mail> getPacketPipeline(LocalDateTime startTime);
  }

  public static class Configuration<NodeInfo> {
    private final Map<String, Node<NodeInfo>> nameToNodeMap = new HashMap<>();
    private final Map<Node<NodeInfo>, WorkerScriptFactory<NodeInfo>> nodeToWorkerScriptFactoryMap =
        new HashMap<>();
    private final Map<NodeConnection<NodeInfo>, PacketPipelineFactory>
        nodeConnectionToPacketPipelineFactoryMap = new HashMap<>();
    private final Collection<Logger> loggers = new LinkedList<>();
    private final NodeInfoGenerator<NodeInfo> nodeInfoGenerator;
    private Clock clock = new VirtualClock(ChronoUnit.MILLIS);
    private boolean hasDatagramRoutingLayer = false;
    private Path crashDumpLocation = null;
    private int port = 0;
    private int datagramBufferSize = 1024;
    private boolean processLoggingEnabled = false;

    public Configuration(NodeInfoGenerator<NodeInfo> nodeInfoGenerator) {
      this.nodeInfoGenerator = nodeInfoGenerator;
    }

    private static boolean isBlank(String string) {
      return string.trim().isEmpty();
    }

    private void addNode(String name, WorkerScriptFactory<NodeInfo> workerScriptFactory) {
      if (name == null) {
        throw new InvalidSimulationConfigurationException("Node name cannot be null.");
      } else if (isBlank(name)) {
        throw new InvalidSimulationConfigurationException("Node name cannot be blank.");
      } else if (nameToNodeMap.containsKey(name)) {
        throw new InvalidSimulationConfigurationException(
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
        throw new InvalidSimulationConfigurationException("Node script cannot be null.");
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
              processLoggingEnabled) -> workerScript);
      return this;
    }

    public Configuration<NodeInfo> addNode(
        String name, WorkerProcessConfiguration workerProcessConfiguration) {
      hasDatagramRoutingLayer = true;
      if (workerProcessConfiguration == null) {
        throw new InvalidSimulationConfigurationException(
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
              processLoggingEnabled) -> {
            WorkerProcessFactory workerProcessFactory =
                workerProcessConfiguration.buildFactory(
                    address, topology, privateIpAddress, workerAddressToPublicIpMap);
            return WorkerDatagramForwardingScript.builder()
                .withWorkerProcessFactory(workerProcessFactory)
                .withPort(port)
                .withPrivateIpAddress(privateIpAddress)
                .withDatagramBufferSize(datagramBufferSize)
                .withPrivateIpAddressToPublicSocketMap(privateIpAddressToPublicSocketMap)
                .withProcessLoggingEnabled(processLoggingEnabled)
                .build();
          });
      return this;
    }

    public Configuration<NodeInfo> addConnection(
        String sourceName, String destinationName, Parameters packetPipelineParameters) {
      if (sourceName == null) {
        throw new InvalidSimulationConfigurationException("Source node name cannot be null.");
      } else if (isBlank(sourceName)) {
        throw new InvalidSimulationConfigurationException("Source node name cannot be blank.");
      } else if (!nameToNodeMap.containsKey(sourceName)) {
        throw new InvalidSimulationConfigurationException(
            "Node with name \"" + sourceName + "\" has not been added yet.");
      } else if (destinationName == null) {
        throw new InvalidSimulationConfigurationException("Destination node name cannot be null.");
      } else if (isBlank(destinationName)) {
        throw new InvalidSimulationConfigurationException("Destination node name cannot be blank.");
      } else if (!nameToNodeMap.containsKey(destinationName)) {
        throw new InvalidSimulationConfigurationException(
            "Node with name \"" + destinationName + "\" has not been added yet.");
      } else if (sourceName.equals(destinationName)) {
        throw new InvalidSimulationConfigurationException("A node cannot be connected to itself.");
      }

      Node<NodeInfo> sourceNode = nameToNodeMap.get(sourceName);
      Node<NodeInfo> destinationNode = nameToNodeMap.get(destinationName);
      NodeConnection<NodeInfo> nodeConnection = new NodeConnection<>(sourceNode, destinationNode);

      if (nodeConnectionToPacketPipelineFactoryMap.containsKey(nodeConnection)) {
        throw new InvalidSimulationConfigurationException("Connection has already been added.");
      }

      nodeConnectionToPacketPipelineFactoryMap.put(
          nodeConnection, startTime -> new PacketPipeline<>(packetPipelineParameters, startTime));
      return this;
    }

    public Configuration<NodeInfo> addLogger(Logger logger) {
      loggers.add(logger);
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

    public DistributedNetworkSimulation<NodeInfo> start() {
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
      UniqueIpAddressGeneratorAdaptor uniqueIpAddressGenerator =
          new UniqueIpAddressGeneratorAdaptor();
      Map<Node<NodeInfo>, DatagramSocket> nodeToPublicSocketMap = new HashMap<>();
      if (this.hasDatagramRoutingLayer) {
        for (Node<NodeInfo> node : nameToNodeMap.values()) {
          try {
            nodeToPublicSocketMap.put(
                node,
                new DatagramSocket(port, uniqueIpAddressGenerator.generateUniqueIpv4Address()));
          } catch (SocketException e) {
            throw new SimulationStartupException(e);
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
                                processLoggingEnabled);
                        return new RunnableNode<>(node, workerScript, nodeInfoGenerator);
                      }));

      // Configure packet pipeline logic.
      LocalDateTime startTime = clock.now();
      Map<NodeConnection<NodeInfo>, PacketPipeline<Mail>> networkConditions =
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
      NetworkSimulatorPostalService<NodeInfo> postalService =
          new NetworkSimulatorPostalService<>(nameToNodeMap.values(), networkConditions);

      DistributedNetworkSimulation<NodeInfo> distributedNetworkSimulation =
          new DistributedNetworkSimulation<>(
              nameToRunnableNodeMap,
              topology,
              postalService,
              clock,
              loggers,
              crashDumpLocation,
              datagramBufferSize,
              privateIpAddressToWorkerAddressMap,
              nodeToPublicSocketMap);

      distributedNetworkSimulation.start();
      return distributedNetworkSimulation;
    }
  }

  private static class UniqueIpAddressGeneratorAdaptor {
    private final UniqueLoopbackIpv4AddressGenerator uniqueLoopbackIpv4AddressGenerator =
        new UniqueLoopbackIpv4AddressGenerator();

    public InetAddress generateUniqueIpv4Address() {
      try {
        InetAddress ipAddress = uniqueLoopbackIpv4AddressGenerator.generateUniqueIpv4Address();
        if (ipAddress == null) {
          throw new SimulationStartupException("The Kernel has run out of fresh ip addresses.");
        }
        return ipAddress;
      } catch (UnknownHostException e) {
        throw new SimulationStartupException(e);
      }
    }
  }
}
