package thorpe.luke.network.simulation.analysis.latency;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLatencyAnalysisSize50 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLatencyAnalysisSize50.class
              .getResource("basic_latency_analysis_size_50.courierconfig")
              .getPath()
        });
  }
}
