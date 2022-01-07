package thorpe.luke.network.simulation.node;

import thorpe.luke.network.simulation.Topology;
import thorpe.luke.time.Clock;

@FunctionalInterface
public interface NodeInfoGenerator<NodeInfo> {
  NodeInfo generateInfo(NodeAddress address, Topology topology, Clock clock);
}
