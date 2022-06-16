package thorpe.luke.network.simulation.analysis.throttle;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicThrottleAnalysisSize5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicThrottleAnalysisSize5.class
              .getResource("basic_throttle_analysis_size_5.courierconfig")
              .getPath()
        });
  }
}
