package thorpe.luke.network.simulation.analysis.control;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicControlAnalysisSize50 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicControlAnalysisSize50.class
              .getResource("basic_control_analysis_size_50.courierconfig")
              .getPath()
        });
  }
}
