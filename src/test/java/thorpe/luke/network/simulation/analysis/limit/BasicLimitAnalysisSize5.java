package thorpe.luke.network.simulation.analysis.limit;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLimitAnalysisSize5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLimitAnalysisSize5.class
              .getResource("basic_limit_analysis_size_5.courierconfig")
              .getPath()
        });
  }
}
