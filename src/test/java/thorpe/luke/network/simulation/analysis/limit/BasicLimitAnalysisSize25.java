package thorpe.luke.network.simulation.analysis.limit;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLimitAnalysisSize25 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLimitAnalysisSize25.class
              .getResource("basic_limit_analysis_size_25.courierconfig")
              .getPath()
        });
  }
}
