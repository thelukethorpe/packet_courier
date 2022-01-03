package thorpe.luke.network.simulator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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
}
