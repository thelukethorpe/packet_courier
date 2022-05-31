package thorpe.luke.network.simulation.analysis.duplication;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDuplicationAnalysisSize100 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDuplicationAnalysisSize100.class
              .getResource("basic_duplication_analysis_size_100.courierconfig")
              .getPath()
        });
  }
}
