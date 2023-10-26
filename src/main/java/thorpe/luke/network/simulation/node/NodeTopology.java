package thorpe.luke.network.simulation.node;

import java.util.*;
import java.util.stream.Collectors;

public class NodeTopology {
  private final Map<String, NodeAddress> nodeNameToAddressMap;
  private final Map<NodeAddress, Collection<NodeAddress>> nodeAddressToNeighboursMap;

  private NodeTopology(
      Map<String, NodeAddress> nodeNameToAddressMap,
      Map<NodeAddress, Collection<NodeAddress>> nodeAddressToNeighboursMap) {
    this.nodeNameToAddressMap = Collections.unmodifiableMap(nodeNameToAddressMap);
    this.nodeAddressToNeighboursMap = Collections.unmodifiableMap(nodeAddressToNeighboursMap);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Collection<NodeAddress> getNodeAddresses() {
    return new HashSet<>(nodeNameToAddressMap.values());
  }

  public NodeAddress getNodeAddress(String nodeName) {
    return nodeNameToAddressMap.get(nodeName);
  }

  public Collection<NodeAddress> getNeighboursOf(String nodeName) {
    return new HashSet<>(
        nodeAddressToNeighboursMap.getOrDefault(new NodeAddress(nodeName), Collections.emptySet()));
  }

  public Collection<NodeAddress> queryByBreadthFirstSearch(String sourceName, int depth) {
    Collection<NodeAddress> visited = new LinkedHashSet<>();
    visited.add(new NodeAddress(sourceName));
    for (int i = 0; i < depth; i++) {
      if (!visited.addAll(
          visited
              .stream()
              .flatMap(nodeName -> getNeighboursOf(nodeName.getName()).stream())
              .collect(Collectors.toList()))) {
        return visited;
      }
    }
    return visited;
  }

  public Collection<NodeAddress> queryByFloodingFrom(String sourceName) {
    return queryByBreadthFirstSearch(sourceName, nodeAddressToNeighboursMap.size());
  }

  public static class Builder {
    final Map<String, NodeAddress> nodeNameToAddressMap = new HashMap<>();
    final Map<NodeAddress, Collection<NodeAddress>> nodeAddressToNeighboursMap = new HashMap<>();

    public NodeTopology build() {
      return new NodeTopology(nodeNameToAddressMap, nodeAddressToNeighboursMap);
    }

    public Builder addNode(String nodeName) {
      nodeNameToAddressMap.computeIfAbsent(nodeName, NodeAddress::new);
      return this;
    }

    public Builder addConnection(String sourceName, String destinationName) {
      NodeAddress sourceAddress = nodeNameToAddressMap.get(sourceName);
      NodeAddress destinationAddress = nodeNameToAddressMap.get(destinationName);
      assert sourceAddress != null;
      assert destinationAddress != null;
      Collection<NodeAddress> sourceNeighbours =
          nodeAddressToNeighboursMap.computeIfAbsent(sourceAddress, nodeAddress -> new HashSet<>());
      sourceNeighbours.add(destinationAddress);
      return this;
    }
  }
}
