package thorpe.luke.network.simulator;

import java.util.*;
import java.util.stream.Collectors;

public class Topology {
  private final Map<String, Collection<String>> nodeToNeighboursMap;

  private Topology(Map<String, Collection<String>> nodeToNeighboursMap) {
    this.nodeToNeighboursMap = nodeToNeighboursMap;
  }

  public static Topology of(Map<String, Collection<String>> nodeToNeighboursMap) {
    Map<String, Collection<String>> immutableNodeToNeighboursMap = new HashMap<>();
    for (Map.Entry<String, Collection<String>> nodeToNeighboursEntry :
        nodeToNeighboursMap.entrySet()) {
      String source = nodeToNeighboursEntry.getKey();
      Collection<String> neighbours = nodeToNeighboursEntry.getValue();
      neighbours
          .stream()
          .filter(node -> !nodeToNeighboursMap.containsKey(node))
          .forEach(node -> immutableNodeToNeighboursMap.put(node, Collections.emptySet()));
      immutableNodeToNeighboursMap.put(source, Collections.unmodifiableCollection(neighbours));
    }
    return new Topology(Collections.unmodifiableMap(immutableNodeToNeighboursMap));
  }

  public Collection<String> getNodes() {
    return new HashSet<>(nodeToNeighboursMap.keySet());
  }

  public Collection<String> getNeighboursOf(String node) {
    return new HashSet<>(nodeToNeighboursMap.getOrDefault(node, Collections.emptySet()));
  }

  public Collection<String> performRadialSearch(String source, int distance) {
    Collection<String> visited = new HashSet<>();
    visited.add(source);
    for (int i = 0; i < distance; i++) {
      if (!visited.addAll(
          visited
              .stream()
              .flatMap(node -> getNeighboursOf(node).stream())
              .collect(Collectors.toList()))) {
        return visited;
      }
    }
    return visited;
  }

  public Collection<String> performFloodSearch(String source) {
    return performRadialSearch(source, nodeToNeighboursMap.size());
  }
}
