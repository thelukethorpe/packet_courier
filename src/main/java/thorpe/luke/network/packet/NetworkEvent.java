package thorpe.luke.network.packet;

import java.util.Arrays;
import java.util.List;

public class NetworkEvent {
  private final double meanInterval;
  private final double meanDuration;
  private final PacketPipeline.Parameters packetPipelineParameters;

  private NetworkEvent(
      double meanInterval,
      double meanDuration,
      PacketPipeline.Parameters packetPipelineParameters) {
    this.meanInterval = meanInterval;
    this.meanDuration = meanDuration;
    this.packetPipelineParameters = packetPipelineParameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public double getMeanInterval() {
    return meanInterval;
  }

  public double getMeanDuration() {
    return meanDuration;
  }

  public PacketPipeline.Parameters getPacketPipelineParameters() {
    return packetPipelineParameters;
  }

  public static class Builder {
    private double meanInterval;
    private double meanDuration;

    public Builder withMeanInterval(double meanInterval) {
      this.meanInterval = meanInterval;
      return this;
    }

    public Builder withMeanDuration(double meanDuration) {
      this.meanDuration = meanDuration;
      return this;
    }

    public NetworkEvent buildWithNetworkConditions(NetworkCondition... networkConditions) {
      return buildWithNetworkConditions(Arrays.asList(networkConditions));
    }

    public NetworkEvent buildWithNetworkConditions(List<NetworkCondition> networkConditions) {
      return new NetworkEvent(
          meanInterval, meanDuration, PacketPipeline.parameters(networkConditions));
    }
  }
}
