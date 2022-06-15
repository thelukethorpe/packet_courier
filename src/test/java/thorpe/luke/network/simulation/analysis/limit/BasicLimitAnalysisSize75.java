package thorpe.luke.network.simulation.analysis.limit;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicLimitAnalysisSize75 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicLimitAnalysisSize75.class
              .getResource("basic_limit_analysis_size_75.courierconfig")
              .getPath()
        });
  }
}
