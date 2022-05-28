package thorpe.luke.network.simulation.analysis.duplication;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDuplicationAnalysisSize25 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDuplicationAnalysisSize25.class
              .getResource("basic_duplication_analysis_size_25.courierconfig")
              .getPath()
        });
  }
}
