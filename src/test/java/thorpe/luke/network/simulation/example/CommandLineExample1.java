package thorpe.luke.network.simulation.example;

import thorpe.luke.network.simulation.PacketCourierMain;

public class CommandLineExample1 {

  public static void main(String[] args) {
    PacketCourierMain.main(
        new String[] {ProtoExample1.class.getResource("cmd_example1.courierconfig").getPath()});
  }
}
