package thorpe.luke.network.simulation.analysis.corruption;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicCorruptionAnalysisSize100 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicCorruptionAnalysisSize100.class
              .getResource("basic_corruption_analysis_size_100.courierconfig")
              .getPath()
        });
  }
}
