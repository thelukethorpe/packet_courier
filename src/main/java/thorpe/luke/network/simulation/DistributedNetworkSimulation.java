package thorpe.luke.network.simulation;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.packet.PacketPipeline.Parameters;
import thorpe.luke.network.simulation.mail.Mail;
import thorpe.luke.network.simulation.mail.NetworkSimulatorPostalService;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.DefaultNodeInfo;
import thorpe.luke.network.simulation.node.Node;
import thorpe.luke.network.simulation.node.NodeConnection;
import thorpe.luke.network.simulation.node.NodeInfoGenerator;
import thorpe.luke.network.simulation.worker.WorkerScript;
import thorpe.luke.time.Clock;
import thorpe.luke.time.VirtualClock;

public class DistributedNetworkSimulation<NodeInfo> {

  private final Thread simulator;

  private DistributedNetworkSimulation(
      Map<String, RunnableNode<NodeInfo>> nodes,
      Topology topology,
      NetworkSimulatorPostalService<NodeInfo> postalService,
      Clock clock,
      Collection<Logger> loggers) {
    this.simulator =
        new Thread(
            () -> {
              AtomicBoolean simulationComplete = new AtomicBoolean(false);
              Collection<Thread> nodeThreads =
                  nodes
                      .values()
                      .stream()
                      .map(
                          runnableNode ->
                              new Thread(
                                  () -> runnableNode.run(topology, postalService, clock, loggers)))
                      .collect(Collectors.toList());
              Thread simulationManagerThread =
                  new Thread(
                      () -> {
                        while (!simulationComplete.get()) {
                          clock.tick();
                          postalService.tick(clock.now());
                        }
                      });
              nodeThreads.forEach(Thread::start);
              simulationManagerThread.start();
              for (Thread nodeThread : nodeThreads) {
                try {
                  nodeThread.join();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
              simulationComplete.set(true);
              try {
                simulationManagerThread.join();
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            });
  }

  public static <NodeInfo> Configuration<NodeInfo> configuration(
      NodeInfoGenerator<NodeInfo> nodeInfoGenerator) {
    return new Configuration<>(nodeInfoGenerator);
  }

  public static Configuration<DefaultNodeInfo> configuration() {
    return new Configuration<>(
        (address, topology, clock) ->
            new DefaultNodeInfo(topology.getNeighboursOf(address.getName()), clock));
  }

  private void start() {
    simulator.start();
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
        Topology topology, PostalService postalService, Clock clock, Collection<Logger> loggers) {
      node.doWork(
          workerScript,
          nodeInfoGenerator.generateInfo(node.getAddress(), topology, clock),
          postalService,
          loggers);
    }

    public Node<NodeInfo> getNode() {
      return node;
    }
  }

  public static class Configuration<NodeInfo> {
    private final Map<String, RunnableNode<NodeInfo>> nodes = new HashMap<>();
    private final Map<NodeConnection<NodeInfo>, PacketPipeline<Mail>> networkConditions =
        new HashMap<>();
    private final Clock clock = new VirtualClock(ChronoUnit.MILLIS);
    private final Collection<Logger> loggers = new LinkedList<>();
    private final NodeInfoGenerator<NodeInfo> nodeInfoGenerator;

    public Configuration(NodeInfoGenerator<NodeInfo> nodeInfoGenerator) {
      this.nodeInfoGenerator = nodeInfoGenerator;
    }

    private static boolean isBlank(String string) {
      return string.trim().isEmpty();
    }

    public Configuration<NodeInfo> addNode(String name, WorkerScript<NodeInfo> workerScript) {
      if (name == null) {
        throw new InvalidSimulationConfigurationException("Node name cannot be null.");
      } else if (isBlank(name)) {
        throw new InvalidSimulationConfigurationException("Node name cannot be blank.");
      } else if (nodes.containsKey(name)) {
        throw new InvalidSimulationConfigurationException(
            "Node with name \"" + name + "\" has already been added.");
      } else if (workerScript == null) {
        throw new InvalidSimulationConfigurationException("Node script cannot be null.");
      }

      Node<NodeInfo> node = new Node<>(name);
      nodes.put(name, new RunnableNode<>(node, workerScript, nodeInfoGenerator));
      NodeConnection<NodeInfo> nodeConnection = new NodeConnection<>(node, node);
      this.networkConditions.put(
          nodeConnection, new PacketPipeline<>(PacketPipeline.perfectParameters(), clock.now()));
      return this;
    }

    public Configuration<NodeInfo> addConnection(
        String sourceName, String destinationName, Parameters packetPipelineParameters) {
      if (sourceName == null) {
        throw new InvalidSimulationConfigurationException("Source node name cannot be null.");
      } else if (isBlank(sourceName)) {
        throw new InvalidSimulationConfigurationException("Source node name cannot be blank.");
      } else if (!nodes.containsKey(sourceName)) {
        throw new InvalidSimulationConfigurationException(
            "Node with name \"" + sourceName + "\" has not been added yet.");
      } else if (destinationName == null) {
        throw new InvalidSimulationConfigurationException("Destination node name cannot be null.");
      } else if (isBlank(destinationName)) {
        throw new InvalidSimulationConfigurationException("Destination node name cannot be blank.");
      } else if (!nodes.containsKey(destinationName)) {
        throw new InvalidSimulationConfigurationException(
            "Node with name \"" + destinationName + "\" has not been added yet.");
      } else if (sourceName.equals(destinationName)) {
        throw new InvalidSimulationConfigurationException("A node cannot be connected to itself.");
      }

      Node<NodeInfo> sourceNode = nodes.get(sourceName).getNode();
      Node<NodeInfo> destinationNode = nodes.get(destinationName).getNode();
      NodeConnection<NodeInfo> nodeConnection = new NodeConnection<>(sourceNode, destinationNode);

      if (this.networkConditions.containsKey(nodeConnection)) {
        throw new InvalidSimulationConfigurationException("Connection has already been added.");
      }

      this.networkConditions.put(
          nodeConnection, new PacketPipeline<>(packetPipelineParameters, clock.now()));
      return this;
    }

    public Configuration<NodeInfo> addLogger(Logger logger) {
      loggers.add(logger);
      return this;
    }

    public DistributedNetworkSimulation<NodeInfo> start() {
      Map<String, Collection<String>> nodeToNeighboursMap = new HashMap<>();
      nodes.keySet().forEach(name -> nodeToNeighboursMap.put(name, new HashSet<>()));
      for (NodeConnection<NodeInfo> nodeConnection : networkConditions.keySet()) {
        String sourceName = nodeConnection.getSource().getAddress().getName();
        String destinationName = nodeConnection.getDestination().getAddress().getName();
        if (!sourceName.equals(destinationName)) {
          nodeToNeighboursMap.get(sourceName).add(destinationName);
        }
      }
      NetworkSimulatorPostalService<NodeInfo> postalService =
          new NetworkSimulatorPostalService<>(
              nodes.values().stream().map(RunnableNode::getNode).collect(Collectors.toList()),
              networkConditions);
      DistributedNetworkSimulation<NodeInfo> distributedNetworkSimulation =
          new DistributedNetworkSimulation<>(
              nodes, Topology.of(nodeToNeighboursMap), postalService, clock, loggers);
      distributedNetworkSimulation.start();
      return distributedNetworkSimulation;
    }
  }
}
