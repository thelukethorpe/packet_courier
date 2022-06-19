package thorpe.luke.network.simulation.analysis.tabulator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import thorpe.luke.log.SimpleLogger;
import thorpe.luke.network.simulation.analysis.aggregator.AggregatedStatistics;

public class AnalysisTabulatorMain {
  private static final int EXIT_CODE_SUCCESS = 0;
  private static final int EXIT_CODE_FAILURE = 1;
  private static final int EXIT_CODE_IO_FAILURE = 2;

  private static final FilenameFilter DIRECTORY_FILTER =
      (current, name) -> new File(current, name).isDirectory();
  private static final FilenameFilter FILE_FILTER =
      (current, name) -> new File(current, name).isFile();

  private static final String SIZE_PREFIX = "Size-";
  private static final Pattern SIZE_REGEX_PATTERN = Pattern.compile(SIZE_PREFIX + "[1-9][0-9]*");

  private static void print(String message, Object... arguments) {
    System.out.printf(message + "\n", arguments);
  }

  private static void exit(int exitCode, String exitMessage, Object... arguments) {
    print(exitMessage, arguments);
    System.exit(exitCode);
  }

  public static void main(String[] args) {
    // Read command line arguments.
    if (args.length == 0) {
      exit(
          EXIT_CODE_IO_FAILURE,
          "Missing arguments: Packet Courier analysis tabulator requires a path to an analysis directory.");
    } else if (args.length > 1) {
      exit(
          EXIT_CODE_IO_FAILURE,
          "Extraneous arguments: Packet Courier analysis tabulator received "
              + args.length
              + " arguments, but only requires a path to an analysis directory.");
    }

    File analysisDirectory = new File(args[0]);
    if (!analysisDirectory.isDirectory()) {
      exit(
          EXIT_CODE_IO_FAILURE,
          "Invalid argument: Packet Courier analysis tabulator requires a path to an analysis directory, not a file.");
      return;
    }

    File[] directoriesToBeTabulated = analysisDirectory.listFiles(DIRECTORY_FILTER);

    if (directoriesToBeTabulated == null || directoriesToBeTabulated.length == 0) {
      exit(
          EXIT_CODE_FAILURE,
          "Failure: %s does not contain any data to be tabulated.",
          analysisDirectory.getPath());
      return;
    }

    SimpleLogger printLogger = new SimpleLogger(AnalysisTabulatorMain::print);
    for (File directoryToBeTabulated : directoriesToBeTabulated) {
      File[] directoriesContainingDataPerMachine =
          directoryToBeTabulated.listFiles(DIRECTORY_FILTER);
      if (directoriesContainingDataPerMachine == null
          || directoriesContainingDataPerMachine.length == 0) {
        exit(
            EXIT_CODE_FAILURE,
            "Failure: %s does not contain any data to be tabulated.",
            directoryToBeTabulated.getPath());
        return;
      }
      String name = directoryToBeTabulated.getName();
      AnalysisTabulator analysisTabulator = new AnalysisTabulator(name);
      for (File directoryContainingDataPerMachine : directoriesContainingDataPerMachine) {
        File[] dataFiles = directoryContainingDataPerMachine.listFiles(FILE_FILTER);
        if (dataFiles == null || dataFiles.length == 0) {
          exit(
              EXIT_CODE_FAILURE,
              "Failure: %s does not contain any data to be tabulated.",
              directoryContainingDataPerMachine.getPath());
          return;
        }
        String machine = directoryContainingDataPerMachine.getName();
        for (File dataFile : dataFiles) {
          String dataFileName = dataFile.getName();
          Matcher sizeMatcher = SIZE_REGEX_PATTERN.matcher(dataFileName);
          sizeMatcher.find();
          int numberOfClients = Integer.parseInt(sizeMatcher.group().replaceFirst(SIZE_PREFIX, ""));
          AggregatedStatistics aggregatedStatistics;
          try {
            aggregatedStatistics = AggregatedStatistics.parse(dataFile, printLogger);
          } catch (IOException e) {
            exit(EXIT_CODE_IO_FAILURE, e.getMessage());
            return;
          }
          analysisTabulator.addColumn(machine, numberOfClients, aggregatedStatistics);
        }
      }
      print("############################################################");
      print("Table for %s:", name);
      print(analysisTabulator.tabulate());
      print("############################################################");
    }
  }
}
