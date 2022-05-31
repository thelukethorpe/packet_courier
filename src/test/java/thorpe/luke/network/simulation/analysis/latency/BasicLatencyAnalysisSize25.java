package thorpe.luke.network.simulation.analysis.latency;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLatencyAnalysisSize25 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLatencyAnalysisSize25.class
              .getResource("basic_latency_analysis_size_25.courierconfig")
              .getPath()
        });
  }
}
