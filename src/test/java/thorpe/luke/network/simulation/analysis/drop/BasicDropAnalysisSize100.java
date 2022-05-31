package thorpe.luke.network.simulation.analysis.drop;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDropAnalysisSize100 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDropAnalysisSize100.class
              .getResource("basic_drop_analysis_size_100.courierconfig")
              .getPath()
        });
  }
}
