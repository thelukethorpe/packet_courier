package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import thorpe.luke.distribution.BernoulliDistribution;
import thorpe.luke.distribution.UniformDistribution;

public interface NetworkCondition {
  static NetworkCondition uniformPacketDrop(double dropProbability, Random random) {
    if (dropProbability < 0.0 || 1.0 < dropProbability) {
      throw new InvalidNetworkConditionException(
          "Packet drop probability should be between 0 and 1.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new SimulatedPacketDropFilter<>(new BernoulliDistribution(dropProbability), random);
      }
    };
  }

  static NetworkCondition uniformPacketLatency(
      double minLatency, double maxLatency, ChronoUnit timeUnit, Random random) {
    if (maxLatency < minLatency) {
      throw new InvalidNetworkConditionException(
          "Minimum latency should be less than or equal to maximum latency.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new SimulatedPacketLatencyFilter<>(
            new UniformDistribution(minLatency, maxLatency), timeUnit, startTime, random);
      }
    };
  }

  <Wrapper extends PacketWrapper<Wrapper>> PacketFilter<Wrapper> asPacketFilterStartingAt(
      LocalDateTime startTime);

  class InvalidNetworkConditionException extends RuntimeException {
    public InvalidNetworkConditionException(String message) {
      super(message);
    }
  }
}
