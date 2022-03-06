package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class PacketPipeline<Wrapper extends PacketWrapper<Wrapper>> {
  private final List<PacketFilter<Wrapper>> packetFilters;

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
    packetFilters.add(0, new ConcurrentNeutralPacketFilter<>());
    this.packetFilters = Collections.unmodifiableList(new ArrayList<>(packetFilters));
  }

  public static Parameters perfectParameters() {
    return parameters();
  }

  public static Parameters parameters(NetworkCondition... networkConditions) {
    return parameters(Arrays.asList(networkConditions));
  }

  public static Parameters parameters(List<NetworkCondition> networkConditions) {
    return new Parameters(networkConditions);
  }

  public void enqueue(Wrapper packetWrapper) {
    packetFilters.get(0).enqueue(packetWrapper);
  }

  public void tick(LocalDateTime now) {
    for (int i = 0; i + 1 < packetFilters.size(); i++) {
      PacketFilter<Wrapper> currentFilter = packetFilters.get(i);
      PacketFilter<Wrapper> nextFilter = packetFilters.get(i + 1);
      currentFilter.tryDequeue().ifPresent(nextFilter::enqueue);
      nextFilter.tick(now);
    }
  }

  public Optional<Wrapper> tryDequeue() {
    PacketFilter<Wrapper> lastFilter = packetFilters.get(packetFilters.size() - 1);
    return lastFilter.tryDequeue();
  }

  public static class Parameters {
    private final List<NetworkCondition> networkConditions;

    private Parameters(List<NetworkCondition> networkConditions) {
      this.networkConditions = networkConditions;
    }

    public List<NetworkCondition> getNetworkConditions() {
      return networkConditions;
    }
  }
}
