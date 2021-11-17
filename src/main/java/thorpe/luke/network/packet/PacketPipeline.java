package thorpe.luke.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PacketPipeline {
  private final ArrayList<PacketFilter> packetFilters;

  public PacketPipeline(Parameters packetPipelineParameters) {
    this(
        packetPipelineParameters.getNetworkConditions().stream()
            .map(NetworkCondition::asPacketFilter)
            .collect(Collectors.toList()));
  }

  private PacketPipeline(List<PacketFilter> packetFilters) {
    this.packetFilters = new ArrayList<>(packetFilters);
    this.packetFilters.add(new NeutralPacketFilter());
  }

  public static Parameters parameters(NetworkCondition... networkConditions) {
    return new Parameters(networkConditions);
  }

  public void enqueue(Packet packet) {
    packetFilters.get(0).enqueue(packet);
  }

  public void tick() {
    for (int i = 0; i + 1 < packetFilters.size(); i++) {
      PacketFilter currentFilter = packetFilters.get(i);
      PacketFilter nextFilter = packetFilters.get(i + 1);
      currentFilter.tryDequeue().ifPresent(nextFilter::enqueue);
    }
  }

  public Optional<Packet> tryDequeue() {
    PacketFilter lastFilter = packetFilters.get(packetFilters.size() - 1);
    return lastFilter.tryDequeue();
  }

  public static class Parameters {
    private final List<NetworkCondition> networkConditions;

    private Parameters(NetworkCondition... networkConditions) {
      this.networkConditions = Arrays.asList(networkConditions);
    }

    public List<NetworkCondition> getNetworkConditions() {
      return networkConditions;
    }
  }
}
