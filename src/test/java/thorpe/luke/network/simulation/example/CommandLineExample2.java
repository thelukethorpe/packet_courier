package thorpe.luke.network.simulation.example;

import thorpe.luke.network.simulation.PacketCourierMain;

public class CommandLineExample2 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          CommandLineExample2.class.getResource("cmd_example2.courierconfig").getPath()
        });
  }
}
