package thorpe.luke.network.simulation.analysis.duplication;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDuplicationAnalysisSize50 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDuplicationAnalysisSize50.class
              .getResource("basic_duplication_analysis_size_50.courierconfig")
              .getPath()
        });
  }
}
