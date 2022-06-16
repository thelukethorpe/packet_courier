package thorpe.luke.network.simulation.analysis.throttle;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicThrottleAnalysisSize25 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicThrottleAnalysisSize25.class
              .getResource("basic_throttle_analysis_size_25.courierconfig")
              .getPath()
        });
  }
}
