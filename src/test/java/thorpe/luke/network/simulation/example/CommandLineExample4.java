package thorpe.luke.network.simulation.example;

import thorpe.luke.network.simulation.PacketCourierMain;

public class CommandLineExample4 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          CommandLineExample4.class.getResource("cmd_example4.courierconfig").getPath()
        });
  }
}
