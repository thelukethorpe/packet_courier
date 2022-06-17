package thorpe.luke.network.simulation.analysis.control;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicControlAnalysisSize100 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicControlAnalysisSize100.class
              .getResource("basic_control_analysis_size_100.courierconfig")
              .getPath()
        });
  }
}
