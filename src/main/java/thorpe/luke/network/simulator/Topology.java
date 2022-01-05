package thorpe.luke.network.simulator;

import java.util.*;
import java.util.stream.Collectors;
import thorpe.luke.network.simulator.node.NodeAddress;

public class Topology {
  private final Map<NodeAddress, Collection<NodeAddress>> nodeToNeighboursMap;

  private Topology(Map<NodeAddress, Collection<NodeAddress>> nodeToNeighboursMap) {
    this.nodeToNeighboursMap = nodeToNeighboursMap;
  }

  public static Topology of(Map<String, Collection<String>> nodeToNeighboursMap) {
    Map<NodeAddress, Collection<NodeAddress>> immutableNodeToNeighboursMap = new HashMap<>();
    for (Map.Entry<String, Collection<String>> nodeToNeighboursEntry :
        nodeToNeighboursMap.entrySet()) {
      NodeAddress sourceAddress = new NodeAddress(nodeToNeighboursEntry.getKey());
      Collection<NodeAddress> neighbours =
          nodeToNeighboursEntry
              .getValue()
              .stream()
              .map(NodeAddress::new)
              .collect(Collectors.toList());
      neighbours
          .stream()
          .filter(node -> !nodeToNeighboursMap.containsKey(node.getName()))
          .forEach(node -> immutableNodeToNeighboursMap.put(node, Collections.emptySet()));
      immutableNodeToNeighboursMap.put(
          sourceAddress, Collections.unmodifiableCollection(neighbours));
    }
    return new Topology(Collections.unmodifiableMap(immutableNodeToNeighboursMap));
  }

  public Collection<NodeAddress> getNodesAddresses() {
    return new HashSet<>(nodeToNeighboursMap.keySet());
  }

  public Collection<NodeAddress> getNeighboursOf(String nodeName) {
    return new HashSet<>(
        nodeToNeighboursMap.getOrDefault(new NodeAddress(nodeName), Collections.emptySet()));
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
    return performRadialSearch(sourceName, nodeToNeighboursMap.size());
  }
}
