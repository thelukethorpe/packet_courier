package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.util.function.Function;

final class ScheduledPacket<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketWrapper<ScheduledPacket<Wrapper>>, Comparable<ScheduledPacket<Wrapper>> {

  private final LocalDateTime scheduledDequeueTime;
  private final Wrapper packetWrapper;

  public ScheduledPacket(LocalDateTime scheduledDequeueTime, Wrapper packetWrapper) {
    this.scheduledDequeueTime = scheduledDequeueTime;
    this.packetWrapper = packetWrapper;
  }

  public LocalDateTime getScheduledDequeueTime() {
    return scheduledDequeueTime;
  }

  public Wrapper getPacketWrapper() {
    return packetWrapper;
  }

  @Override
  public int compareTo(ScheduledPacket that) {
    return this.scheduledDequeueTime.compareTo(that.scheduledDequeueTime);
  }

  @Override
  public ScheduledPacket<Wrapper> map(Function<Packet, Packet> function) {
    return new ScheduledPacket<>(scheduledDequeueTime, packetWrapper.map(function));
  }

  @Override
  public ScheduledPacket<Wrapper> copy() {
    return new ScheduledPacket<>(scheduledDequeueTime, packetWrapper.copy());
  }

  @Override
  public Packet getPacket() {
    return packetWrapper.getPacket();
  }
}
