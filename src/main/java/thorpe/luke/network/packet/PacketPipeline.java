package thorpe.luke.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PacketPipeline implements PacketFilter {
  private final ArrayList<PacketFilter> packetFilters;

  public PacketPipeline(PacketFilter... packetFilters) {
    this(Arrays.asList(packetFilters));
  }

  public PacketPipeline(List<PacketFilter> packetFilters) {
    this.packetFilters = new ArrayList<>(packetFilters);
    this.packetFilters.add(new NeutralPacketFilter());
  }

  @Override
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

  @Override
  public Optional<Packet> tryDequeue() {
    PacketFilter lastFilter = packetFilters.get(packetFilters.size() - 1);
    return lastFilter.tryDequeue();
  }
}
