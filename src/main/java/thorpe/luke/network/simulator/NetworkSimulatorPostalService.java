package thorpe.luke.network.simulator;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;

public class NetworkSimulatorPostalService<NodeInfo> implements PostalService {
  private final Map<String, Node<NodeInfo>> addressToNode;
  private final ConcurrentMap<Connection<NodeInfo>, PacketPipeline> networkConditions;

  NetworkSimulatorPostalService(
      Collection<Node<NodeInfo>> nodes,
      Map<Connection<NodeInfo>, PacketPipeline> networkConditions) {
    this.addressToNode =
        nodes.stream().collect(Collectors.toMap(Node::getName, Function.identity()));
    this.networkConditions = new ConcurrentHashMap<>(networkConditions);
  }

  public void tick(LocalDateTime now) {
    for (Entry<Connection<NodeInfo>, PacketPipeline> networkConditionEntry :
        networkConditions.entrySet()) {
      Connection<NodeInfo> connection = networkConditionEntry.getKey();
      PacketPipeline packetPipeline = networkConditionEntry.getValue();
      packetPipeline.tick(now);
      packetPipeline.tryDequeue().ifPresent(packet -> connection.getDestination().deliver(packet));
    }
  }

  @Override
  public boolean mail(String sourceAddress, String destinationAddress, Packet packet) {
    Node<NodeInfo> source = addressToNode.get(sourceAddress);
    Node<NodeInfo> destination = addressToNode.get(destinationAddress);
    if (source == null || destination == null) {
      return false;
    }
    PacketPipeline packetPipeline = networkConditions.get(new Connection<>(source, destination));
    if (packetPipeline == null) {
      return false;
    }
    packetPipeline.enqueue(packet);
    return true;
  }
}
