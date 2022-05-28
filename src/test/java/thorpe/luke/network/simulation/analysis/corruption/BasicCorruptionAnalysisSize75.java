package thorpe.luke.network.simulation.analysis.corruption;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicCorruptionAnalysisSize75 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicCorruptionAnalysisSize75.class
              .getResource("basic_corruption_analysis_size_75.courierconfig")
              .getPath()
        });
  }
}
