package thorpe.luke.network.simulator;

@FunctionalInterface
public interface NodeScript {
  void run(NodeState nodeState);
}
