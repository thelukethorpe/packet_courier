package thorpe.luke.network.simulation.analysis.throttle;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicThrottleAnalysisSize75 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicThrottleAnalysisSize75.class
              .getResource("basic_throttle_analysis_size_75.courierconfig")
              .getPath()
        });
  }
}
