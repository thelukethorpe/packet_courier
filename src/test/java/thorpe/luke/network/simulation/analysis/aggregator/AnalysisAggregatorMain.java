package thorpe.luke.network.simulation.analysis.aggregator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    // Set up buffered reader to poll lines from log file.
    BufferedReader bufferedReader;
    try {
      bufferedReader = new BufferedReader(new FileReader(args[0]));
    } catch (FileNotFoundException e) {
      exit(EXIT_CODE_IO_FAILURE, "Log file not found: " + e.getMessage());
      return;
    }

    // Prepare data structures to aggregate data from log file.
    Map<UniquePacketId, AnalysisClientPacket> uniquePacketIdToClientPacketMap = new HashMap<>();
    Map<UniquePacketId, List<AnalysisServerPacket>> uniquePacketIdToServerPacketsMap =
        new HashMap<>();
    int numberOfJunkPackets = 0;

    // Parse data from log file.
    print("Info: Parsing data from log file...");
    do {
      String logLine;
      try {
        logLine = bufferedReader.readLine();
        if (logLine == null) {
          break;
        }
        try {
          if (logLine.startsWith(AnalysisClientPacket.PREFIX)) {
            AnalysisClientPacket analysisClientPacket = AnalysisClientPacket.parse(logLine);
            uniquePacketIdToClientPacketMap.put(
                analysisClientPacket.getUniquePacketId(), analysisClientPacket);
          } else if (logLine.startsWith(AnalysisServerPacket.PREFIX)) {
            AnalysisServerPacket analysisServerPacket = AnalysisServerPacket.parse(logLine);
            uniquePacketIdToServerPacketsMap.putIfAbsent(
                analysisServerPacket.getUniquePacketId(), new LinkedList<>());
            uniquePacketIdToServerPacketsMap
                .get(analysisServerPacket.getUniquePacketId())
                .add(analysisServerPacket);
          } else {
            throw new AnalysisMessageParseException();
          }
        } catch (AnalysisMessageParseException e) {
          numberOfJunkPackets++;
        }
      } catch (IOException e) {
        exit(EXIT_CODE_IO_FAILURE, "Failed to read from log file: " + e.getMessage());
        return;
      }
    } while (true);

    // Aggregate data into useful statistics.
    print("Info: Aggregating parsed data...");
    int numberOfPacketsSentByClients = uniquePacketIdToClientPacketMap.size();
    int numberOfPacketsReceivedByServer = numberOfJunkPackets;
    int numberOfUnexpectedArrivals = 0;
    int numberOfMissingArrivals = 0;
    int numberOfChecksumCorruptedPackets = 0;
    List<Long> packetLatenciesInMilliseconds = new LinkedList<>();
    LocalDateTime timeOfFirstPacketSend = LocalDateTime.MAX;
    LocalDateTime timeOfLastPacketReceipt = LocalDateTime.MIN;

    for (Map.Entry<UniquePacketId, AnalysisClientPacket> packetIdToClientPacketEntry :
        uniquePacketIdToClientPacketMap.entrySet()) {
      UniquePacketId uniquePacketId = packetIdToClientPacketEntry.getKey();
      AnalysisClientPacket clientPacket = packetIdToClientPacketEntry.getValue();
      if (clientPacket.getDateTimeOfSending().isBefore(timeOfFirstPacketSend)) {
        timeOfFirstPacketSend = clientPacket.getDateTimeOfSending();
      }

      List<AnalysisServerPacket> serverPackets =
          uniquePacketIdToServerPacketsMap.get(uniquePacketId);
      uniquePacketIdToServerPacketsMap.remove(uniquePacketId);
      if (serverPackets == null || serverPackets.isEmpty()) {
        numberOfMissingArrivals++;
        continue;
      }
      numberOfPacketsReceivedByServer += serverPackets.size();
      numberOfUnexpectedArrivals += serverPackets.size() - 1;
      for (AnalysisServerPacket serverPacket : serverPackets) {
        if (serverPacket.isChecksumCorrupted()) {
          numberOfChecksumCorruptedPackets++;
          continue;
        }
        if (timeOfLastPacketReceipt.isBefore(serverPacket.getDateTimeOfReceipt())) {
          timeOfLastPacketReceipt = serverPacket.getDateTimeOfReceipt();
        }
        long packetLatencyInMilliseconds =
            ChronoUnit.MILLIS.between(
                clientPacket.getDateTimeOfSending(), serverPacket.getDateTimeOfReceipt());
        packetLatenciesInMilliseconds.add(packetLatencyInMilliseconds);
      }
    }

    long durationInMilliseconds =
        ChronoUnit.MILLIS.between(timeOfFirstPacketSend, timeOfLastPacketReceipt);
    HourMinuteSecondMillisecondDuration durationInHoursMinutesSecondsMilliseconds =
        HourMinuteSecondMillisecondDuration.fromMilliseconds(durationInMilliseconds);
    int leftoverPackets =
        uniquePacketIdToServerPacketsMap.values().stream().mapToInt(List::size).sum();
    numberOfPacketsReceivedByServer += leftoverPackets;
    numberOfChecksumCorruptedPackets += leftoverPackets;
    double meanUnexpectedArrivalsPerSend =
        numberOfUnexpectedArrivals / (double) numberOfPacketsSentByClients;
    double meanPacketLatencyInMilliseconds =
        packetLatenciesInMilliseconds.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
    double missingArrivalRate = numberOfMissingArrivals / (double) numberOfPacketsSentByClients;
    double packetChecksumCorruptionRate =
        numberOfChecksumCorruptedPackets / (double) numberOfPacketsReceivedByServer;
    double packetJunkRate = numberOfJunkPackets / (double) numberOfPacketsReceivedByServer;
    double packetSendRatePerMillisecond =
        numberOfPacketsSentByClients / (double) durationInMilliseconds;
    double packetReceiptRatePerMillisecond =
        numberOfPacketsReceivedByServer / (double) durationInMilliseconds;
    double meanPacketSizeInBytes =
        uniquePacketIdToClientPacketMap
            .values()
            .stream()
            .map(AnalysisClientPacket::getSize)
            .mapToDouble(Integer::doubleValue)
            .average()
            .orElse(0.0);

    // Print aggregated statistics.
    print("Info: Aggregation complete - printing results of analysis...");
    print("############################################################");
    print("Duration: %s", durationInHoursMinutesSecondsMilliseconds);
    print("############################################################");
    print("Number of packets sent by clients:     %d", numberOfPacketsSentByClients);
    print("Number of packets received by server:  %d", numberOfPacketsReceivedByServer);
    print("Number of unexpected arrivals:         %d", numberOfUnexpectedArrivals);
    print("Number of missing arrivals:            %d", numberOfMissingArrivals);
    print("Number of checksum corrupted packets:  %d", numberOfChecksumCorruptedPackets);
    print("Number of junk packets:                %d", numberOfJunkPackets);
    print("############################################################");
    print("Mean latency of packets (ms):          %.2f", meanPacketLatencyInMilliseconds);
    print("Mean unexpected arrivals per send:     %.2f", meanUnexpectedArrivalsPerSend);
    print("Missing arrival rate:                  %.2f%%", 100 * missingArrivalRate);
    print("Packet checksum corruption rate:       %.2f%%", 100 * packetChecksumCorruptionRate);
    print("Packet junk rate:                      %.2f%%", 100 * packetJunkRate);
    print(
        "Overall packet corruption rate:        %.2f%%",
        100 * (packetChecksumCorruptionRate + packetJunkRate));
    print("Packet send rate (ms⁻¹):               %.2f", packetSendRatePerMillisecond);
    print("Packet receipt rate (ms⁻¹):            %.2f", packetReceiptRatePerMillisecond);
    print("Mean packet size (byte):               %.2f", meanPacketSizeInBytes);
    print("############################################################");
    print("Unexpected Arrivals:");
    print("   Packets with IDs that the server has already seen.");
    print("Missing Arrivals:");
    print(
        "   Packets that were sent by a client but couldn't be reliably identified by the server. This could either be because they were dropped or because they were junk upon arrival.");
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
      return String.format(
          "%d hours, %d minutes, %d.%03d seconds", hours, minutes, seconds, milliseconds);
    }
  }
}
