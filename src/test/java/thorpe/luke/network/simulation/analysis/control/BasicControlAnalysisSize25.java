package thorpe.luke.network.simulation.analysis.control;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicControlAnalysisSize25 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicControlAnalysisSize25.class
              .getResource("basic_control_analysis_size_25.courierconfig")
              .getPath()
        });
  }
}
