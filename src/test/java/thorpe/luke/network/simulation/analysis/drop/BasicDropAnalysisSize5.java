package thorpe.luke.network.simulation.analysis.drop;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDropAnalysisSize5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDropAnalysisSize5.class
              .getResource("basic_drop_analysis_size_5.courierconfig")
              .getPath()
        });
  }
}
