package thorpe.luke.network.simulation.analysis.corruption;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicCorruptionAnalysisSize50 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicCorruptionAnalysisSize50.class
              .getResource("basic_corruption_analysis_size_50.courierconfig")
              .getPath()
        });
  }
}
