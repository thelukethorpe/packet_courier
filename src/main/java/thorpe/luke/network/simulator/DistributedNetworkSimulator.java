package thorpe.luke.network.simulator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import thorpe.luke.network.packet.PacketPipeline;

public class DistributedNetworkSimulator {

  private final Map<String, RunnableNode> nodes;
  private final NetworkSimulatorPostalService postalService;

  private DistributedNetworkSimulator(
      Map<String, RunnableNode> nodes, NetworkSimulatorPostalService postalService) {
    this.nodes = nodes;
    this.postalService = postalService;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void start() {
    AtomicBoolean simulationComplete = new AtomicBoolean(false);
    Collection<Thread> nodeThreads =
        nodes.values().stream()
            .map(runnableNode -> new Thread(() -> runnableNode.run(postalService)))
            .collect(Collectors.toList());
    Thread networkThread =
        new Thread(
            () -> {
              while (!simulationComplete.get()) {
                postalService.tick();
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
  }

  private static class RunnableNode {
    private final Node node;
    private final NodeScript nodeScript;

    private RunnableNode(Node node, NodeScript nodeScript) {
      this.node = node;
      this.nodeScript = nodeScript;
    }

    public void run(PostalService postalService) {
      node.run(nodeScript, postalService);
    }

    public Node getNode() {
      return node;
    }
  }

  public static class Builder {
    private final Map<String, RunnableNode> nodes = new HashMap<>();
    private final Map<Connection, PacketPipeline> networkConditions = new HashMap<>();

    private static boolean isBlank(String string) {
      return string.trim().isEmpty();
    }

    public Builder addNode(String name, NodeScript nodeScript)
        throws InvalidSimulatorConfigurationException {
      if (name == null) {
        throw new InvalidSimulatorConfigurationException("Node name cannot be null.");
      } else if (isBlank(name)) {
        throw new InvalidSimulatorConfigurationException("Node name cannot be blank.");
      } else if (nodes.containsKey(name)) {
        throw new InvalidSimulatorConfigurationException(
            "Node with name \"" + name + "\" has already been added.");
      } else if (nodeScript == null) {
        throw new InvalidSimulatorConfigurationException("Node script cannot be null.");
      }

      Node node = new Node(name);
      nodes.put(name, new RunnableNode(node, nodeScript));
      return this;
    }

    public Builder addConnection(
        String sourceName, String destinationName, PacketPipeline packetPipeline)
        throws InvalidSimulatorConfigurationException {
      if (sourceName == null) {
        throw new InvalidSimulatorConfigurationException("Source node name cannot be null.");
      } else if (isBlank(sourceName)) {
        throw new InvalidSimulatorConfigurationException("Source node name cannot be blank.");
      } else if (!nodes.containsKey(sourceName)) {
        throw new InvalidSimulatorConfigurationException(
            "Node with name \"" + sourceName + "\" has not been added yet.");
      } else if (destinationName == null) {
        throw new InvalidSimulatorConfigurationException("Destination node name cannot be null.");
      } else if (isBlank(destinationName)) {
        throw new InvalidSimulatorConfigurationException("Destination node name cannot be blank.");
      } else if (!nodes.containsKey(destinationName)) {
        throw new InvalidSimulatorConfigurationException(
            "Node with name \"" + destinationName + "\" has not been added yet.");
      } else if (packetPipeline == null) {
        throw new InvalidSimulatorConfigurationException("Packet pipeline cannot be null.");
      }

      Node sourceNode = nodes.get(sourceName).getNode();
      Node destinationNode = nodes.get(destinationName).getNode();
      Connection connection = new Connection(sourceNode, destinationNode);

      if (networkConditions.containsKey(connection)) {
        throw new InvalidSimulatorConfigurationException("Connection has already been added.");
      }

      networkConditions.put(connection, packetPipeline);
      return this;
    }

    public DistributedNetworkSimulator build() {
      NetworkSimulatorPostalService postalService =
          new NetworkSimulatorPostalService(
              nodes.values().stream().map(RunnableNode::getNode).collect(Collectors.toList()),
              networkConditions);
      return new DistributedNetworkSimulator(nodes, postalService);
    }
  }

  public static class InvalidSimulatorConfigurationException extends Exception {
    public InvalidSimulatorConfigurationException(String message) {
      super(message);
    }
  }
}
