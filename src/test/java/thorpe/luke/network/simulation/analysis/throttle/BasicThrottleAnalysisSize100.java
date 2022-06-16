package thorpe.luke.network.simulation.analysis.throttle;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicThrottleAnalysisSize100 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicThrottleAnalysisSize100.class
              .getResource("basic_throttle_analysis_size_100.courierconfig")
              .getPath()
        });
  }
}
