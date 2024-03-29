package thorpe.luke.network.simulation.mail;

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
import thorpe.luke.network.simulation.node.Node;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.node.NodeConnection;
import thorpe.luke.network.simulation.worker.WorkerAddress;

public class PacketCourierPostalService implements PostalService {
  private final Map<NodeAddress, Node> nodeToAddressMap;
  private final ConcurrentMap<NodeConnection, PacketPipeline<Mail>>
      nodeConnectionToPacketPipelineMap;

  public PacketCourierPostalService(
      Collection<Node> nodes,
      Map<NodeConnection, PacketPipeline<Mail>> nodeConnectionToPacketPipelineMap) {
    this.nodeToAddressMap =
        nodes.stream().collect(Collectors.toMap(Node::getAddress, Function.identity()));
    this.nodeConnectionToPacketPipelineMap =
        new ConcurrentHashMap<>(nodeConnectionToPacketPipelineMap);
  }

  public void tick(LocalDateTime now) {
    for (Entry<NodeConnection, PacketPipeline<Mail>> networkConditionEntry :
        nodeConnectionToPacketPipelineMap.entrySet()) {
      NodeConnection nodeConnection = networkConditionEntry.getKey();
      PacketPipeline<Mail> packetPipeline = networkConditionEntry.getValue();
      packetPipeline.tick(now);
      packetPipeline.tryDequeue().ifPresent(mail -> nodeConnection.getDestination().deliver(mail));
    }
  }

  @Override
  public boolean mail(
      WorkerAddress sourceAddress, WorkerAddress destinationAddress, Packet packet) {
    Node source = nodeToAddressMap.get(sourceAddress.getHostingNodeAddress());
    Node destination = nodeToAddressMap.get(destinationAddress.getHostingNodeAddress());
    if (source == null || destination == null) {
      return false;
    }
    PacketPipeline<Mail> packetPipeline =
        nodeConnectionToPacketPipelineMap.get(new NodeConnection(source, destination));
    if (packetPipeline == null) {
      return false;
    }
    packetPipeline.enqueue(new Mail(destinationAddress, packet));
    return true;
  }
}
