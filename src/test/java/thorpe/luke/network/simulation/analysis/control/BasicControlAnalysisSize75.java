package thorpe.luke.network.simulation.analysis.control;

import thorpe.luke.network.simulation.PacketCourierMain;

public class BasicControlAnalysisSize75 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          BasicControlAnalysisSize75.class
              .getResource("basic_control_analysis_size_75.courierconfig")
              .getPath()
        });
  }
}
