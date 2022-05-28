package thorpe.luke.network.simulation.analysis.duplication;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDuplicationAnalysisSize5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDuplicationAnalysisSize5.class
              .getResource("basic_duplication_analysis_size_5.courierconfig")
              .getPath()
        });
  }
}
