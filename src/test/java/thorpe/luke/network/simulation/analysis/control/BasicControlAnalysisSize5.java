package thorpe.luke.network.simulation.analysis.control;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicControlAnalysisSize5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicControlAnalysisSize5.class
              .getResource("basic_control_analysis_size_5.courierconfig")
              .getPath()
        });
  }
}
