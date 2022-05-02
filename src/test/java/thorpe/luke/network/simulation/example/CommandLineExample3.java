package thorpe.luke.network.simulation.example;

import thorpe.luke.network.simulation.PacketCourierMain;

public class CommandLineExample3 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          CommandLineExample3.class.getResource("cmd_example3.courierconfig").getPath()
        });
  }
}
