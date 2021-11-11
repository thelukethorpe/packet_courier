package thorpe.luke.network.simulator;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.packet.PacketPipeline;

public class NetworkSimulatorPostalService implements PostalService {
  private final Map<String, Node> addressToNode;
  private final ConcurrentMap<Connection, PacketPipeline> networkConditions;

  NetworkSimulatorPostalService(
      Collection<Node> nodes, Map<Connection, PacketPipeline> networkConditions) {
    this.addressToNode =
        nodes.stream().collect(Collectors.toMap(Node::getName, Function.identity()));
    this.networkConditions = new ConcurrentHashMap<>(networkConditions);
  }

  public void tick() {
    for (Entry<Connection, PacketPipeline> networkConditionEntry : networkConditions.entrySet()) {
      Connection connection = networkConditionEntry.getKey();
      PacketPipeline packetPipeline = networkConditionEntry.getValue();
      packetPipeline.tick();
      packetPipeline.tryDequeue().ifPresent(packet -> connection.getDestination().deliver(packet));
    }
  }

  @Override
  public boolean mail(String sourceAddress, String destinationAddress, Packet packet) {
    Node source = addressToNode.get(sourceAddress);
    Node destination = addressToNode.get(destinationAddress);
    if (source == null || destination == null) {
      return false;
    }
    PacketPipeline packetPipeline = networkConditions.get(new Connection(source, destination));
    if (packetPipeline == null) {
      return false;
    }
    packetPipeline.enqueue(packet);
    return true;
  }
}
