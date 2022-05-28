package thorpe.luke.network.simulation.analysis.drop;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDropAnalysisSize50 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDropAnalysisSize50.class
              .getResource("basic_drop_analysis_size_50.courierconfig")
              .getPath()
        });
  }
}
