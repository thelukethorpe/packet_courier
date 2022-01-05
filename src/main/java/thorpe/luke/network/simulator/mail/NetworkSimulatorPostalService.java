package thorpe.luke.network.simulator.mail;

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
import thorpe.luke.network.simulator.node.Node;
import thorpe.luke.network.simulator.node.NodeAddress;
import thorpe.luke.network.simulator.node.NodeConnection;
import thorpe.luke.network.simulator.worker.WorkerAddress;

public class NetworkSimulatorPostalService<NodeInfo> implements PostalService {
  private final Map<NodeAddress, Node<NodeInfo>> nodeToAddressMap;
  private final ConcurrentMap<NodeConnection<NodeInfo>, PacketPipeline<Mail>> networkConditions;

  public NetworkSimulatorPostalService(
      Collection<Node<NodeInfo>> nodes,
      Map<NodeConnection<NodeInfo>, PacketPipeline<Mail>> networkConditions) {
    this.nodeToAddressMap =
        nodes.stream().collect(Collectors.toMap(Node::getAddress, Function.identity()));
    this.networkConditions = new ConcurrentHashMap<>(networkConditions);
  }

  public void tick(LocalDateTime now) {
    for (Entry<NodeConnection<NodeInfo>, PacketPipeline<Mail>> networkConditionEntry :
        networkConditions.entrySet()) {
      NodeConnection<NodeInfo> nodeConnection = networkConditionEntry.getKey();
      PacketPipeline<Mail> packetPipeline = networkConditionEntry.getValue();
      packetPipeline.tick(now);
      packetPipeline
          .tryDequeue()
          .ifPresent(packet -> nodeConnection.getDestination().deliver(packet));
    }
  }

  @Override
  public boolean mail(
      WorkerAddress sourceAddress, WorkerAddress destinationAddress, Packet packet) {
    Node<NodeInfo> source = nodeToAddressMap.get(sourceAddress.getHostingNodeAddress());
    Node<NodeInfo> destination = nodeToAddressMap.get(destinationAddress.getHostingNodeAddress());
    if (source == null || destination == null) {
      return false;
    }
    PacketPipeline<Mail> packetPipeline =
        networkConditions.get(new NodeConnection<>(source, destination));
    if (packetPipeline == null) {
      return false;
    }
    packetPipeline.enqueue(new Mail(destinationAddress, packet));
    return true;
  }
}
