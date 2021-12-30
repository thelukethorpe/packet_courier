package thorpe.luke.network.simulator;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.packet.PacketPipeline.Parameters;
import thorpe.luke.time.Clock;
import thorpe.luke.time.VirtualClock;

public class DistributedNetworkSimulation {

  private final Thread simulator;

  private DistributedNetworkSimulation(
      Map<String, RunnableNode> nodes,
      Map<String, Collection<String>> topology,
      NetworkSimulatorPostalService postalService,
      Clock clock) {
    this.simulator =
        new Thread(
            () -> {
              AtomicBoolean simulationComplete = new AtomicBoolean(false);
              Collection<Thread> nodeThreads =
                  nodes
                      .values()
                      .stream()
                      .map(
                          runnableNode -> {
                            String nodeName = runnableNode.getNode().getName();
                            Collection<String> neighbours = topology.get(nodeName);
                            return new Thread(
                                () -> runnableNode.run(neighbours, postalService, clock));
                          })
                      .collect(Collectors.toList());
              Thread networkThread =
                  new Thread(
                      () -> {
                        while (!simulationComplete.get()) {
                          clock.tick();
                          postalService.tick(clock.now());
                        }
                      });
              nodeThreads.forEach(Thread::start);
              networkThread.start();
              for (Thread nodeThread : nodeThreads) {
                try {
                  nodeThread.join();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
              simulationComplete.set(true);
              try {
                networkThread.join();
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            });
  }

  public static Configuration configuration() {
    return new Configuration();
  }

  private void start() {
    simulator.start();
  }

  public void join() throws InterruptedException {
    simulator.join();
  }

  private static class RunnableNode {
    private final Node node;
    private final NodeScript nodeScript;

    private RunnableNode(Node node, NodeScript nodeScript) {
      this.node = node;
      this.nodeScript = nodeScript;
    }

    public void run(Collection<String> neighbours, PostalService postalService, Clock clock) {
      node.run(nodeScript, neighbours, postalService, clock);
    }

    public Node getNode() {
      return node;
    }
  }

  public static class Configuration {
    private final Map<String, RunnableNode> nodes = new HashMap<>();
    private final Map<Connection, PacketPipeline> networkConditions = new HashMap<>();
    private final Clock clock = new VirtualClock(ChronoUnit.MILLIS);

    private static boolean isBlank(String string) {
      return string.trim().isEmpty();
    }

    public Configuration addNode(String name, NodeScript nodeScript)
        throws InvalidSimulationConfigurationException {
      if (name == null) {
        throw new InvalidSimulationConfigurationException("Node name cannot be null.");
      } else if (isBlank(name)) {
        throw new InvalidSimulationConfigurationException("Node name cannot be blank.");
      } else if (nodes.containsKey(name)) {
        throw new InvalidSimulationConfigurationException(
            "Node with name \"" + name + "\" has already been added.");
      } else if (nodeScript == null) {
        throw new InvalidSimulationConfigurationException("Node script cannot be null.");
      }

      Node node = new Node(name);
      nodes.put(name, new RunnableNode(node, nodeScript));
      return this;
    }

    public Configuration addConnection(
        String sourceName, String destinationName, Parameters packetPipelineParameters)
        throws InvalidSimulationConfigurationException {
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
      }

      Node sourceNode = nodes.get(sourceName).getNode();
      Node destinationNode = nodes.get(destinationName).getNode();
      Connection connection = new Connection(sourceNode, destinationNode);

      if (this.networkConditions.containsKey(connection)) {
        throw new InvalidSimulationConfigurationException("Connection has already been added.");
      }

      this.networkConditions.put(
          connection, new PacketPipeline(packetPipelineParameters, clock.now()));
      return this;
    }

    public DistributedNetworkSimulation start() {
      Map<String, Collection<String>> topology = new HashMap<>();
      nodes.keySet().forEach(name -> topology.put(name, new HashSet<>()));
      for (Connection connection : networkConditions.keySet()) {
        String sourceName = connection.getSource().getName();
        String destinationName = connection.getDestination().getName();
        topology.get(sourceName).add(destinationName);
      }
      NetworkSimulatorPostalService postalService =
          new NetworkSimulatorPostalService(
              nodes.values().stream().map(RunnableNode::getNode).collect(Collectors.toList()),
              networkConditions);
      DistributedNetworkSimulation distributedNetworkSimulation =
          new DistributedNetworkSimulation(nodes, topology, postalService, clock);
      distributedNetworkSimulation.start();
      return distributedNetworkSimulation;
    }
  }

  public static class InvalidSimulationConfigurationException extends Exception {
    public InvalidSimulationConfigurationException(String message) {
      super(message);
    }
  }
}
