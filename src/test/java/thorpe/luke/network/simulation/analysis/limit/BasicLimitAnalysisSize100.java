package thorpe.luke.network.simulation.analysis.limit;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLimitAnalysisSize100 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLimitAnalysisSize100.class
              .getResource("basic_limit_analysis_size_100.courierconfig")
              .getPath()
        });
  }
}
