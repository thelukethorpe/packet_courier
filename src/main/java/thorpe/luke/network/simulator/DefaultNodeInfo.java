package thorpe.luke.network.simulator;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import thorpe.luke.time.Clock;

public class DefaultNodeInfo {
  private final String name;
  private final Collection<String> neighbours;
  private final Clock clock;

  public DefaultNodeInfo(String name, Collection<String> neighbours, Clock clock) {
    this.name = name;
    this.neighbours = Collections.unmodifiableCollection(neighbours);
    this.clock = clock;
  }

  public String getName() {
    return name;
  }

  public Collection<String> getNeighbours() {
    return neighbours;
  }

  public LocalDateTime getCurrentTime() {
    return clock.now();
  }
}
