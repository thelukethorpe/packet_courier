package thorpe.luke.network.simulation.analysis.drop;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicDropAnalysisSize75 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicDropAnalysisSize75.class
              .getResource("basic_drop_analysis_size_75.courierconfig")
              .getPath()
        });
  }
}
