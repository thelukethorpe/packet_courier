package thorpe.luke.network.simulation.analysis.latency;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLatencyAnalysisSize5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLatencyAnalysisSize5.class
              .getResource("basic_latency_analysis_size_5.courierconfig")
              .getPath()
        });
  }
}
