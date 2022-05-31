package thorpe.luke.network.simulation.analysis.corruption;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicCorruptionAnalysisSize5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicCorruptionAnalysisSize5.class
              .getResource("basic_corruption_analysis_size_5.courierconfig")
              .getPath()
        });
  }
}
