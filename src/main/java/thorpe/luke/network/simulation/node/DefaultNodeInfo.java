package thorpe.luke.network.simulation.node;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import thorpe.luke.time.Clock;

public class DefaultNodeInfo {
  private final Collection<NodeAddress> neighbours;
  private final Clock clock;

  public DefaultNodeInfo(Collection<NodeAddress> neighbours, Clock clock) {
    this.neighbours = Collections.unmodifiableCollection(neighbours);
    this.clock = clock;
  }

  public static NodeInfoGenerator<DefaultNodeInfo> generator() {
    return (address, topology, clock) ->
        new DefaultNodeInfo(topology.getNeighboursOf(address.getName()), clock);
  }

  public Collection<NodeAddress> getNeighbours() {
    return neighbours;
  }

  public LocalDateTime getCurrentTime() {
    return clock.now();
  }
}
