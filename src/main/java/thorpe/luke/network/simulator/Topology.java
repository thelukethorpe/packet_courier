package thorpe.luke.network.simulator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class Topology {
  private final Map<String, Collection<String>> nodeToNeighboursMap;

  public Topology(Map<String, Collection<String>> nodeToNeighboursMap) {
    this.nodeToNeighboursMap = nodeToNeighboursMap;
  }

  public Collection<String> getNodes() {
    return Collections.unmodifiableCollection(nodeToNeighboursMap.keySet());
  }

  public Collection<String> getNeighboursOf(String node) {
    return Collections.unmodifiableCollection(nodeToNeighboursMap.get(node));
  }

  public Collection<String> performRadialSearch(String source, int distance) {
    Collection<String> visited = new HashSet<>();
    visited.add(source);
    for (int i = 0; i < distance; i++) {
      visited.addAll(
          visited
              .stream()
              .flatMap(node -> getNeighboursOf(node).stream())
              .collect(Collectors.toList()));
    }
    return visited;
  }
}
