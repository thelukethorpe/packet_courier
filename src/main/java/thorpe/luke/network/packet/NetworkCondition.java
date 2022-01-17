package thorpe.luke.network.packet;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import thorpe.luke.distribution.*;

public interface NetworkCondition {
  static NetworkCondition packetLimit(int packetLimit) {
    if (packetLimit < 0) {
      throw new InvalidNetworkConditionException(
          "Packet limit should be greater than or equal to 0.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new PacketLimitingFilter<>(packetLimit);
      }
    };
  }

  static NetworkCondition packetThrottle(int throttleRate, ChronoUnit timeUnit) {
    if (throttleRate < 0) {
      throw new InvalidNetworkConditionException(
          "Packet throttle rate should be greater than or equal to 0.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new PacketThrottlingFilter<>(throttleRate, timeUnit, startTime);
      }
    };
  }

  static NetworkCondition uniformPacketCorruption(double corruptionProbability, Random random) {
    if (corruptionProbability < 0.0 || 1.0 < corruptionProbability) {
      throw new InvalidNetworkConditionException(
          "Packet corruption probability should be between 0 and 1.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new SimulatedPacketCorruptionFilter<>(
            new BernoulliDistribution(corruptionProbability),
            new UniformIntegerDistribution(0, Integer.MAX_VALUE),
            new UniformIntegerDistribution(0, Integer.MAX_VALUE),
            random);
      }
    };
  }

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

  static NetworkCondition poissonPacketDuplication(double meanDuplications, Random random) {
    if (meanDuplications < 0.0) {
      throw new InvalidNetworkConditionException(
          "Mean duplications should be greater than or equal to zero.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new SimulatedPacketDuplicationFilter<>(
            new PoissonDistribution(meanDuplications), random);
      }
    };
  }

  static NetworkCondition normalPacketLatency(
      double meanLatency, double standardDeviation, ChronoUnit timeUnit, Random random) {
    if (meanLatency < 0.0) {
      throw new InvalidNetworkConditionException(
          "Mean latency should be greater than or equal to zero.");
    } else if (standardDeviation <= 0.0) {
      throw new InvalidNetworkConditionException(
          "Latency standard-deviation should be greater than zero.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new SimulatedPacketLatencyFilter<>(
            new NormalDistribution(meanLatency, standardDeviation), timeUnit, startTime, random);
      }
    };
  }

  static NetworkCondition exponentialPacketLatency(
      double meanLatency, ChronoUnit timeUnit, Random random) {
    if (meanLatency <= 0.0) {
      throw new InvalidNetworkConditionException("Mean latency should be greater than zero.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new SimulatedPacketLatencyFilter<>(
            new ExponentialDistribution(meanLatency), timeUnit, startTime, random);
      }
    };
  }

  static NetworkCondition uniformPacketLatency(
      double minLatency, double maxLatency, ChronoUnit timeUnit, Random random) {
    if (minLatency < 0.0) {
      throw new InvalidNetworkConditionException(
          "Minimum latency should be greater than or equal to zero.");
    } else if (maxLatency < minLatency) {
      throw new InvalidNetworkConditionException(
          "Minimum latency should be less than or equal to maximum latency.");
    }
    return new NetworkCondition() {
      @Override
      public <Wrapper extends PacketWrapper<Wrapper>>
          PacketFilter<Wrapper> asPacketFilterStartingAt(LocalDateTime startTime) {
        return new SimulatedPacketLatencyFilter<>(
            new UniformRealDistribution(minLatency, maxLatency), timeUnit, startTime, random);
      }
    };
  }

  <Wrapper extends PacketWrapper<Wrapper>> PacketFilter<Wrapper> asPacketFilterStartingAt(
      LocalDateTime startTime);
}
