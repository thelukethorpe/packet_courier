package thorpe.luke.network.simulation.analysis.aggregator;

import java.util.Objects;

public class UniquePacketId {
  private final int packetId;
  private final String clientName;

  public UniquePacketId(int packetId, String clientName) {
    this.packetId = packetId;
    this.clientName = clientName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(packetId, clientName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof UniquePacketId) {
      UniquePacketId that = (UniquePacketId) obj;
      return this.packetId == that.packetId && Objects.equals(this.clientName, that.clientName);
    }
    return false;
  }
}
