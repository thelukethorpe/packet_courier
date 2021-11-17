package thorpe.luke.network.packet;

import java.time.temporal.ChronoUnit;
import java.util.Random;
import thorpe.luke.distribution.BernoulliDistribution;
import thorpe.luke.distribution.UniformDistribution;

@FunctionalInterface
public interface NetworkCondition {
  static NetworkCondition uniformPacketDrop(double dropProbability, Random random)
      throws InvalidNetworkConditionException {
    if (dropProbability < 0.0 || 1.0 < dropProbability) {
      throw new InvalidNetworkConditionException(
          "Packet drop probability should be between 0 and 1.");
    }
    return () -> new SimulatedPacketDropFilter(new BernoulliDistribution(dropProbability, random));
  }

  static NetworkCondition uniformPacketLatency(
      double minLatency, double maxLatency, ChronoUnit timeUnit, Random random)
      throws InvalidNetworkConditionException {
    if (maxLatency < minLatency) {
      throw new InvalidNetworkConditionException(
          "Minimum latency should be less than or equal to maximum latency.");
    }
    return () ->
        new SimulatedPacketLatencyFilter(
            new UniformDistribution(minLatency, maxLatency, random), timeUnit);
  }

  PacketFilter asPacketFilter();

  class InvalidNetworkConditionException extends Exception {
    public InvalidNetworkConditionException(String message) {
      super(message);
    }
  }
}
