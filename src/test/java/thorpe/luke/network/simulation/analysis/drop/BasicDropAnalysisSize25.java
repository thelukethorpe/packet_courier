package thorpe.luke.network.simulation.analysis.drop;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDropAnalysisSize25 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDropAnalysisSize25.class
              .getResource("basic_drop_analysis_size_25.courierconfig")
              .getPath()
        });
  }
}
