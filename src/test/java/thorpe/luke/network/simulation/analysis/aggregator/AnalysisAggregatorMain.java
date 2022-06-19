package thorpe.luke.network.simulation.analysis.aggregator;

import java.io.*;
import java.time.Duration;
import java.util.*;
import thorpe.luke.log.SimpleLogger;

public class AnalysisAggregatorMain {
  private static final int EXIT_CODE_SUCCESS = 0;
  private static final int EXIT_CODE_FAILURE = 1;
  private static final int EXIT_CODE_IO_FAILURE = 2;

  private static void print(String message, Object... arguments) {
    System.out.printf(message + "\n", arguments);
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
          "Missing arguments: Packet Courier analysis aggregator requires a path to an analysis log.");
    } else if (args.length > 1) {
      exit(
          EXIT_CODE_IO_FAILURE,
          "Extraneous arguments: Packet Courier analysis aggregator received "
              + args.length
              + " arguments, but only requires a path to an analysis log.");
    }

    File file = new File(args[0]);
    AggregatedStatistics aggregatedStatistics;
    try {
      aggregatedStatistics =
          AggregatedStatistics.parse(file, new SimpleLogger(AnalysisAggregatorMain::print));
    } catch (IOException e) {
      exit(EXIT_CODE_IO_FAILURE, e.getMessage());
      return;
    }

    // Print aggregated statistics.
    print("Info: Aggregation complete - printing results of analysis...");
    print("############################################################");
    print(
        "Duration: %s",
        HourMinuteSecondMillisecondDuration.fromMilliseconds(
            aggregatedStatistics.getDurationInMilliseconds()));
    print("############################################################");
    print(
        "Number of packets sent by clients:     %d",
        aggregatedStatistics.getNumberOfPacketsSentByClients());
    print(
        "Number of packets received by server:  %d",
        aggregatedStatistics.getNumberOfPacketsReceivedByServer());
    print(
        "Number of unexpected arrivals:         %d",
        aggregatedStatistics.getNumberOfUnexpectedArrivals());
    print(
        "Number of missing arrivals:            %d",
        aggregatedStatistics.getNumberOfMissingArrivals());
    print(
        "Number of unidentifiable arrivals:     %d",
        aggregatedStatistics.getNumberOfUnidentifiableArrivals());
    print(
        "Number of checksum corrupted packets:  %d",
        aggregatedStatistics.getNumberOfChecksumCorruptedPackets());
    print(
        "Number of junk packets:                %d", aggregatedStatistics.getNumberOfJunkPackets());
    print("############################################################");
    print(
        "Mean latency of packets (ms):          %.2f",
        aggregatedStatistics.getMeanPacketLatencyInMilliseconds());
    print(
        "Mean unexpected arrivals per send:     %.2f",
        aggregatedStatistics.getMeanUnexpectedArrivalsPerSend());
    print(
        "Missing arrival rate:                  %.2f%%",
        100 * aggregatedStatistics.getMissingArrivalRate());
    print(
        "Unidentifiable arrival rate:           %.2f%%",
        100 * aggregatedStatistics.getUnidentifiableArrivalRate());
    print(
        "Packet checksum corruption rate:       %.2f%%",
        100 * aggregatedStatistics.getPacketChecksumCorruptionRate());
    print(
        "Packet junk rate:                      %.2f%%",
        100 * aggregatedStatistics.getPacketJunkRate());
    print(
        "Packet send rate (ms⁻¹):               %.2f",
        aggregatedStatistics.getPacketSendRatePerMillisecond());
    print(
        "Packet receipt rate (ms⁻¹):            %.2f",
        aggregatedStatistics.getPacketReceiptRatePerMillisecond());
    print(
        "Mean packet size (byte):               %.2f",
        aggregatedStatistics.getMeanPacketSizeInBytes());
    print("############################################################");
    print(
        "Bytes received during middle quantiles:    %.2f",
        aggregatedStatistics.getBytesReceivedByServerDuringMiddleQuantiles());
    print(
        "Bytes rate during middle quantiles (ms⁻¹): %.2f",
        aggregatedStatistics.getByteRateDuringMiddleQuantiles());
    print("############################################################");
    print("Unexpected Arrivals:");
    print("   Packets with IDs that the server has already seen.");
    print("Missing Arrivals:");
    print(
        "   Packets that were sent by a client but couldn't be reliably identified by the server. This could either be because they were dropped or because they were junk upon arrival.");
    print("Unidentifiable Arrivals:");
    print("   Packets IDs that didn't match anything sent by clients.");
    print("Checksum Corrupted Packets:");
    print("   Packets that had an invalid checksum upon arrival.");
    print("Junk Packets:");
    print(
        "   Packets that could not be parsed by the server, i.e.: had crucial structural issues due to corruption which makes their contents unintelligible.");

    // Analysis aggregation complete.
    print("############################################################");
    exit(EXIT_CODE_SUCCESS, "Success: Packet Courier analysis aggregation has finished.");
  }

  private static class HourMinuteSecondMillisecondDuration {
    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int milliseconds;

    private HourMinuteSecondMillisecondDuration(
        int hours, int minutes, int seconds, int milliseconds) {
      this.hours = hours;
      this.minutes = minutes;
      this.seconds = seconds;
      this.milliseconds = milliseconds;
    }

    private static HourMinuteSecondMillisecondDuration fromMilliseconds(long milliseconds) {
      long hours = Duration.ofMillis(milliseconds).toHours();
      milliseconds -= Duration.ofHours(hours).toMillis();
      long minutes = Duration.ofMillis(milliseconds).toMinutes();
      milliseconds -= Duration.ofMinutes(minutes).toMillis();
      long seconds = Duration.ofMillis(milliseconds).getSeconds();
      milliseconds -= Duration.ofSeconds(hours).toMillis();
      return new HourMinuteSecondMillisecondDuration(
          (int) hours, (int) minutes, (int) seconds, (int) milliseconds);
    }

    @Override
    public String toString() {
      return String.format("%d.%03d seconds", 3600 * hours + 60 * minutes + seconds, milliseconds);
    }
  }
}
