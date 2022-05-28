package thorpe.luke.network.simulation.analysis.latency;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLatencyAnalysisSize75 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLatencyAnalysisSize75.class
              .getResource("basic_latency_analysis_size_75.courierconfig")
              .getPath()
        });
  }
}
