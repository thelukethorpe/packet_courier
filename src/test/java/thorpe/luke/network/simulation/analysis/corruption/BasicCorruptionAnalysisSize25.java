package thorpe.luke.network.simulation.analysis.corruption;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicCorruptionAnalysisSize25 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicCorruptionAnalysisSize25.class
              .getResource("basic_corruption_analysis_size_25.courierconfig")
              .getPath()
        });
  }
}
