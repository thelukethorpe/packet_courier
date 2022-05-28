package thorpe.luke.network.simulation.analysis.duplication;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDuplicationAnalysisSize75 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDuplicationAnalysisSize75.class
              .getResource("basic_duplication_analysis_size_75.courierconfig")
              .getPath()
        });
  }
}
