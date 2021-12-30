package thorpe.luke.network.simulator;

import java.util.Collection;
import java.util.Map;
import thorpe.luke.time.Clock;

@FunctionalInterface
public interface NodeInfoGenerator<NodeInfo> {
  NodeInfo generateInfo(String name, Map<String, Collection<String>> topology, Clock clock);
}
