package thorpe.luke.network.simulation.example;

import thorpe.luke.network.simulation.PacketCourierMain;

public class CommandLineExample5 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {
          CommandLineExample5.class.getResource("cmd_example5.courierconfig").getPath()
        });
  }
}
