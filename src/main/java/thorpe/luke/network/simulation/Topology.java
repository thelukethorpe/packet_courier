package thorpe.luke.network.simulation;

import java.util.*;
import java.util.stream.Collectors;
import thorpe.luke.network.simulation.node.NodeAddress;

public class Topology {
  private final Map<String, NodeAddress> nodeNameToAddressMap;
  private final Map<NodeAddress, Collection<NodeAddress>> nodeAddressToNeighboursMap;

  private Topology(
      Map<String, NodeAddress> nodeNameToAddressMap,
      Map<NodeAddress, Collection<NodeAddress>> nodeAddressToNeighboursMap) {
    this.nodeNameToAddressMap = Collections.unmodifiableMap(nodeNameToAddressMap);
    this.nodeAddressToNeighboursMap = Collections.unmodifiableMap(nodeAddressToNeighboursMap);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Collection<NodeAddress> getNodesAddresses() {
    return new HashSet<>(nodeNameToAddressMap.values());
  }

  public NodeAddress getNodeAddress(String nodeName) {
    return nodeNameToAddressMap.get(nodeName);
  }

  public Collection<NodeAddress> getNeighboursOf(String nodeName) {
    return new HashSet<>(
        nodeAddressToNeighboursMap.getOrDefault(new NodeAddress(nodeName), Collections.emptySet()));
  }

  public Collection<NodeAddress> performRadialSearch(String sourceName, int distance) {
    Collection<NodeAddress> visited = new HashSet<>();
    visited.add(new NodeAddress(sourceName));
    for (int i = 0; i < distance; i++) {
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

  public Collection<NodeAddress> performFloodSearch(String sourceName) {
    return performRadialSearch(sourceName, nodeAddressToNeighboursMap.size());
  }

  public static class Builder {
    final Map<String, NodeAddress> nodeNameToAddressMap = new HashMap<>();
    final Map<NodeAddress, Collection<NodeAddress>> nodeAddressToNeighboursMap = new HashMap<>();

    public Topology build() {
      return new Topology(nodeNameToAddressMap, nodeAddressToNeighboursMap);
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
