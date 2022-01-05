package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PacketPipeline<Wrapper extends PacketWrapper<Wrapper>> {
  private final ArrayList<PacketFilter<Wrapper>> packetFilters;

  public PacketPipeline(Parameters packetPipelineParameters, LocalDateTime startTime) {
    this(
        packetPipelineParameters
            .getNetworkConditions()
            .stream()
            .map(
                networkCondition ->
                    (PacketFilter<Wrapper>) networkCondition.asPacketFilterStartingAt(startTime))
            .collect(Collectors.toList()));
  }

  private PacketPipeline(List<PacketFilter<Wrapper>> packetFilters) {
    this.packetFilters = new ArrayList<>(packetFilters);
    this.packetFilters.add(new NeutralPacketFilter<>());
  }

  public static Parameters parameters(NetworkCondition... networkConditions) {
    return new Parameters(networkConditions);
  }

  public void enqueue(Wrapper packetWrapper) {
    packetFilters.get(0).enqueue(packetWrapper);
  }

  public void tick(LocalDateTime now) {
    for (int i = 0; i + 1 < packetFilters.size(); i++) {
      PacketFilter<Wrapper> currentFilter = packetFilters.get(i);
      PacketFilter<Wrapper> nextFilter = packetFilters.get(i + 1);
      currentFilter.tick(now);
      currentFilter.tryDequeue().ifPresent(nextFilter::enqueue);
    }
  }

  public Optional<Wrapper> tryDequeue() {
    PacketFilter<Wrapper> lastFilter = packetFilters.get(packetFilters.size() - 1);
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
