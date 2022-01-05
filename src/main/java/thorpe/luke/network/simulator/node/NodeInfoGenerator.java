package thorpe.luke.network.simulator.node;

import thorpe.luke.network.simulator.Topology;
import thorpe.luke.time.Clock;

@FunctionalInterface
public interface NodeInfoGenerator<NodeInfo> {
  NodeInfo generateInfo(NodeAddress address, Topology topology, Clock clock);
}
