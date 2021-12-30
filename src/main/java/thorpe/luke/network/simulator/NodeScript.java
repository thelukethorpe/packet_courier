package thorpe.luke.network.simulator;

@FunctionalInterface
public interface NodeScript<NodeInfo> {
  void run(NodeManager<NodeInfo> nodeManager);
}
