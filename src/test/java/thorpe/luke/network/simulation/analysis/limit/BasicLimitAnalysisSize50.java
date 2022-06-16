package thorpe.luke.network.simulation.analysis.limit;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLimitAnalysisSize50 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLimitAnalysisSize50.class
              .getResource("basic_limit_analysis_size_50.courierconfig")
              .getPath()
        });
  }
}
