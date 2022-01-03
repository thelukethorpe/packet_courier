package thorpe.luke.network.simulator;

import thorpe.luke.time.Clock;

@FunctionalInterface
public interface NodeInfoGenerator<NodeInfo> {
  NodeInfo generateInfo(String name, Topology topology, Clock clock);
}
