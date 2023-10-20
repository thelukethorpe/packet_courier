package thorpe.luke.network.simulation;

import java.io.File;
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

  private static class ArgParseException extends Exception {
    public ArgParseException(String message) {
      super(message);
    }
  }

  private static class ParsedArgs {
    private final String configurationFilePath;

    private ParsedArgs(String configurationFilePath) {
      this.configurationFilePath = configurationFilePath;
    }

    public static ParsedArgs parse(String[] args) throws ArgParseException {
      if (args.length == 0) {
        throw new ArgParseException(
            "Missing arguments: Packet Courier requires a path to a "
                + PacketCourierSimulation.CONFIGURATION_FILE_EXTENSION
                + " file.");
      } else if (args.length > 1) {
        throw new ArgParseException(
            "Extraneous arguments: Packet Courier received "
                + args.length
                + " arguments, but only requires a path to a "
                + PacketCourierSimulation.CONFIGURATION_FILE_EXTENSION
                + " file.");
      }
      return new ParsedArgs(args[0]);
    }

    public String getConfigurationFilePath() {
      return configurationFilePath;
    }
  }

  public static void main(String[] args) {
    // Parse command-line arguments.
    ParsedArgs parsedArgs;
    try {
      parsedArgs = ParsedArgs.parse(args);
    } catch (ArgParseException e) {
      exit(EXIT_CODE_IO_FAILURE, "Failed to parse command-line arguments: " + e.getMessage());
      return;
    }

    // Parse simulation configuration from file.
    print("Info: Processing Packet Courier configuration file...");
    PacketCourierSimulation.Configuration configuration;
    try {
      configuration =
          PacketCourierSimulationConfigurationProtoParser.parse(
              new File(parsedArgs.getConfigurationFilePath()));
    } catch (PacketCourierSimulationConfigurationProtoParserException e) {
      exit(EXIT_CODE_IO_FAILURE, "Failed to parse configuration file: " + e.getMessage());
      return;

    } catch (PacketCourierSimulationConfigurationException e) {
      exit(EXIT_CODE_FAILURE, "Failed to configure simulation: " + e.getMessage());
      return;
    }

    // Configure the simulation.
    PacketCourierSimulation simulation;
    try {
      simulation = configuration.configure();
    } catch (PacketCourierSimulationConfigurationException e) {
      exit(EXIT_CODE_FAILURE, "Failed to configure simulation: " + e.getMessage());
      return;
    }

    // Start the simulation.
    print("Info: Starting Packet Courier simulation...");
    print("#################### START ####################");

    simulation.run();

    // Simulation complete.
    print("#################### FINISH ####################");
    exit(EXIT_CODE_SUCCESS, "Success: Packet Courier simulation has finished.");
  }
}
