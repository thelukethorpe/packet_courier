package thorpe.luke.network.simulation.analysis.latency;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLatencyAnalysisSize100 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLatencyAnalysisSize100.class
              .getResource("basic_latency_analysis_size_100.courierconfig")
              .getPath()
        });
  }
}
