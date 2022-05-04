package thorpe.luke.network.simulation;

import java.io.File;
import thorpe.luke.network.simulation.node.DefaultNodeInfo;
import thorpe.luke.network.simulation.proto.PacketCourierSimulationConfigurationProtoParser;
import thorpe.luke.network.simulation.proto.PacketCourierSimulationConfigurationProtoParserException;

public class PacketCourierMain {
  private static final int EXIT_CODE_SUCCESS = 0;
  private static final int EXIT_CODE_FAILURE = 1;
  private static final int EXIT_CODE_IO_FAILURE = 2;
  private static final int EXIT_CODE_FATAL_ERROR_SIGNAL_FAILURE = 130;

  private static void print(String message) {
    System.out.println(message);
  }

  private static void exit(int exitCode, String exitMessage) {
    print(exitMessage);
    System.exit(exitCode);
  }

  public static void main(String[] args) {
    // Read command line arguments.
    if (args.length == 0) {
      exit(
          EXIT_CODE_IO_FAILURE,
          "Missing arguments: Packet Courier requires a path to a "
              + PacketCourierSimulation.CONFIGURATION_FILE_EXTENSION
              + " file.");
    } else if (args.length > 1) {
      exit(
          EXIT_CODE_IO_FAILURE,
          "Extraneous arguments: Packet Courier received "
              + args.length
              + " arguments, but only requires a path to a "
              + PacketCourierSimulation.CONFIGURATION_FILE_EXTENSION
              + " file.");
    }
    File packetCourierSimulationConfigurationFile = new File(args[0]);

    // Parse simulation configuration from file.
    print("Info: Processing Packet Courier configuration file...");
    PacketCourierSimulation.Configuration<DefaultNodeInfo> packetCourierSimulationConfiguration;
    try {
      packetCourierSimulationConfiguration =
          PacketCourierSimulationConfigurationProtoParser.parse(
              packetCourierSimulationConfigurationFile);
    } catch (PacketCourierSimulationConfigurationProtoParserException
        | PacketCourierSimulationConfigurationException e) {
      exit(EXIT_CODE_FAILURE, "Configuration error: " + e.getMessage());
      return;
    }

    // Configure the simulation.
    PacketCourierSimulation<DefaultNodeInfo> packetCourierSimulation;
    try {
      packetCourierSimulation = packetCourierSimulationConfiguration.configure();
    } catch (PacketCourierSimulationConfigurationException e) {
      exit(EXIT_CODE_FAILURE, "Configuration error: " + e.getMessage());
      return;
    }

    // Start the simulation.
    print("Info: Starting Packet Courier simulation...");
    print("#################### START ####################");
    packetCourierSimulation.start();

    // Wait for the simulation to complete.
    try {
      packetCourierSimulation.waitFor();
    } catch (InterruptedException e) {
      exit(
          EXIT_CODE_FATAL_ERROR_SIGNAL_FAILURE,
          "Fatal: Packet Courier simulation was interrupted.");
    }

    // Simulation complete.
    print("#################### FINISH ####################");
    exit(EXIT_CODE_SUCCESS, "Success: Packet Courier simulation has finished.");
  }
}
