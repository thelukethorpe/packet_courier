package thorpe.luke.network.packet;

import java.time.temporal.ChronoUnit;
import java.util.Random;
import thorpe.luke.distribution.BernoulliDistribution;
import thorpe.luke.distribution.UniformDistribution;

@FunctionalInterface
public interface NetworkConditions {
  static NetworkConditions uniformPacketDrop(double dropProbability, Random random)
      throws InvalidNetworkConditionsException {
    if (dropProbability < 0.0 || 1.0 < dropProbability) {
      throw new InvalidNetworkConditionsException(
          "Packet drop probability should be between 0 and 1.");
    }
    return () -> new SimulatedPacketDropFilter(new BernoulliDistribution(dropProbability, random));
  }

  static NetworkConditions uniformPacketLatency(
      double minLatency, double maxLatency, ChronoUnit timeUnit, Random random)
      throws InvalidNetworkConditionsException {
    if (maxLatency < minLatency) {
      throw new InvalidNetworkConditionsException(
          "Minimum latency should be less than or equal to maximum latency.");
    }
    return () ->
        new SimulatedPacketLatencyFilter(
            new UniformDistribution(minLatency, maxLatency, random), timeUnit);
  }

  PacketFilter asPacketFilter();

  class InvalidNetworkConditionsException extends Exception {
    public InvalidNetworkConditionsException(String message) {
      super(message);
    }
  }
}
