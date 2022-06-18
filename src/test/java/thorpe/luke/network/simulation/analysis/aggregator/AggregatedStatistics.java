package thorpe.luke.network.simulation.analysis.aggregator;

import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import thorpe.luke.log.Logger;

public class AggregatedStatistics {
  private final long durationInMilliseconds;

  private final int numberOfPacketsSentByClients;
  private final int numberOfPacketsReceivedByServer;
  private final int numberOfUnexpectedArrivals;
  private final int numberOfMissingArrivals;
  private final int numberOfUnidentifiableArrivals;
  private final int numberOfChecksumCorruptedPackets;
  private final int numberOfJunkPackets;

  private final double meanPacketLatencyInMilliseconds;
  private final double meanUnexpectedArrivalsPerSend;
  private final double missingArrivalRate;
  private final double unidentifiableArrivalRate;
  private final double packetChecksumCorruptionRate;
  private final double packetJunkRate;
  private final double packetSendRatePerMillisecond;
  private final double packetReceiptRatePerMillisecond;
  private final double meanPacketSizeInBytes;

  private final double bytesReceivedByServerDuringMiddleQuantiles;
  private final double byteRateDuringMiddleQuantiles;

  private AggregatedStatistics(
      int numberOfPacketsSentByClients,
      int numberOfPacketsReceivedByServer,
      int numberOfUnexpectedArrivals,
      int numberOfMissingArrivals,
      int numberOfChecksumCorruptedPackets,
      int numberOfJunkPackets,
      long durationInMilliseconds,
      int numberOfUnidentifiableArrivals,
      double meanUnexpectedArrivalsPerSend,
      double missingArrivalRate,
      double meanPacketLatencyInMilliseconds,
      double unidentifiableArrivalRate,
      double packetChecksumCorruptionRate,
      double packetJunkRate,
      double packetSendRatePerMillisecond,
      double packetReceiptRatePerMillisecond,
      double meanPacketSizeInBytes,
      double bytesReceivedByServerDuringMiddleQuantiles,
      double byteRateDuringMiddleQuantiles) {
    this.numberOfPacketsSentByClients = numberOfPacketsSentByClients;
    this.numberOfPacketsReceivedByServer = numberOfPacketsReceivedByServer;
    this.numberOfUnexpectedArrivals = numberOfUnexpectedArrivals;
    this.numberOfMissingArrivals = numberOfMissingArrivals;
    this.numberOfChecksumCorruptedPackets = numberOfChecksumCorruptedPackets;
    this.numberOfJunkPackets = numberOfJunkPackets;
    this.durationInMilliseconds = durationInMilliseconds;
    this.numberOfUnidentifiableArrivals = numberOfUnidentifiableArrivals;
    this.meanUnexpectedArrivalsPerSend = meanUnexpectedArrivalsPerSend;
    this.missingArrivalRate = missingArrivalRate;
    this.meanPacketLatencyInMilliseconds = meanPacketLatencyInMilliseconds;
    this.unidentifiableArrivalRate = unidentifiableArrivalRate;
    this.packetChecksumCorruptionRate = packetChecksumCorruptionRate;
    this.packetJunkRate = packetJunkRate;
    this.packetSendRatePerMillisecond = packetSendRatePerMillisecond;
    this.packetReceiptRatePerMillisecond = packetReceiptRatePerMillisecond;
    this.meanPacketSizeInBytes = meanPacketSizeInBytes;
    this.bytesReceivedByServerDuringMiddleQuantiles = bytesReceivedByServerDuringMiddleQuantiles;
    this.byteRateDuringMiddleQuantiles = byteRateDuringMiddleQuantiles;
  }

  public static AggregatedStatistics parse(File file, Logger logger) throws IOException {
    // Set up buffered reader to poll lines from log file.
    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

    // Prepare data structures to aggregate data from log file.
    Map<UniquePacketId, AnalysisClientPacket> uniquePacketIdToClientPacketMap = new HashMap<>();
    Map<UniquePacketId, List<AnalysisServerPacket>> uniquePacketIdToServerPacketsMap =
        new HashMap<>();
    TreeMap<LocalDateTime, List<AnalysisServerPacket>> dateTimeOfReceiptToServerPacketsMap =
        new TreeMap<>();
    int numberOfJunkPackets = 0;

    // Parse data from log file.
    logger.log("Info: Parsing data from log file...");
    do {
      String logLine;
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
          dateTimeOfReceiptToServerPacketsMap.putIfAbsent(
              analysisServerPacket.getDateTimeOfReceipt(), new LinkedList<>());
          dateTimeOfReceiptToServerPacketsMap
              .get(analysisServerPacket.getDateTimeOfReceipt())
              .add(analysisServerPacket);
        } else {
          throw new AnalysisMessageParseException();
        }
      } catch (AnalysisMessageParseException e) {
        numberOfJunkPackets++;
      }
    } while (true);

    // Aggregate data into useful statistics.
    logger.log("Info: Aggregating parsed data...");
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
    int numberOfUnidentifiableArrivals =
        uniquePacketIdToServerPacketsMap.values().stream().mapToInt(List::size).sum();
    numberOfPacketsReceivedByServer += numberOfUnidentifiableArrivals;
    numberOfChecksumCorruptedPackets += numberOfUnidentifiableArrivals;
    double meanUnexpectedArrivalsPerSend =
        numberOfUnexpectedArrivals / (double) numberOfPacketsSentByClients;
    double meanPacketLatencyInMilliseconds =
        packetLatenciesInMilliseconds.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
    double missingArrivalRate = numberOfMissingArrivals / (double) numberOfPacketsSentByClients;
    double unidentifiableArrivalRate =
        numberOfUnidentifiableArrivals / (double) numberOfPacketsSentByClients;
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

    long quantileDurationInMilliseconds = durationInMilliseconds / 4;
    LocalDateTime startTimeOfSecondQuantile =
        timeOfFirstPacketSend.plus(quantileDurationInMilliseconds, ChronoUnit.MILLIS);
    LocalDateTime finishTimeOfThirdQuantile =
        timeOfLastPacketReceipt.minus(quantileDurationInMilliseconds, ChronoUnit.MILLIS);
    double bytesReceivedByServerDuringMiddleQuantiles =
        dateTimeOfReceiptToServerPacketsMap
            .subMap(startTimeOfSecondQuantile, finishTimeOfThirdQuantile)
            .values()
            .stream()
            .mapToDouble(serverPackets -> serverPackets.size() * meanPacketSizeInBytes)
            .sum();
    double byteRateDuringMiddleQuantiles =
        bytesReceivedByServerDuringMiddleQuantiles / (quantileDurationInMilliseconds * 2.0);

    return new AggregatedStatistics(
        numberOfPacketsSentByClients,
        numberOfPacketsReceivedByServer,
        numberOfUnexpectedArrivals,
        numberOfMissingArrivals,
        numberOfChecksumCorruptedPackets,
        numberOfJunkPackets,
        durationInMilliseconds,
        numberOfUnidentifiableArrivals,
        meanUnexpectedArrivalsPerSend,
        missingArrivalRate,
        meanPacketLatencyInMilliseconds,
        unidentifiableArrivalRate,
        packetChecksumCorruptionRate,
        packetJunkRate,
        packetSendRatePerMillisecond,
        packetReceiptRatePerMillisecond,
        meanPacketSizeInBytes,
        bytesReceivedByServerDuringMiddleQuantiles,
        byteRateDuringMiddleQuantiles);
  }

  public int getNumberOfPacketsSentByClients() {
    return numberOfPacketsSentByClients;
  }

  public int getNumberOfPacketsReceivedByServer() {
    return numberOfPacketsReceivedByServer;
  }

  public int getNumberOfUnexpectedArrivals() {
    return numberOfUnexpectedArrivals;
  }

  public int getNumberOfMissingArrivals() {
    return numberOfMissingArrivals;
  }

  public int getNumberOfChecksumCorruptedPackets() {
    return numberOfChecksumCorruptedPackets;
  }

  public int getNumberOfJunkPackets() {
    return numberOfJunkPackets;
  }

  public long getDurationInMilliseconds() {
    return durationInMilliseconds;
  }

  public int getNumberOfUnidentifiableArrivals() {
    return numberOfUnidentifiableArrivals;
  }

  public double getMeanUnexpectedArrivalsPerSend() {
    return meanUnexpectedArrivalsPerSend;
  }

  public double getMissingArrivalRate() {
    return missingArrivalRate;
  }

  public double getMeanPacketLatencyInMilliseconds() {
    return meanPacketLatencyInMilliseconds;
  }

  public double getUnidentifiableArrivalRate() {
    return unidentifiableArrivalRate;
  }

  public double getPacketChecksumCorruptionRate() {
    return packetChecksumCorruptionRate;
  }

  public double getPacketJunkRate() {
    return packetJunkRate;
  }

  public double getPacketSendRatePerMillisecond() {
    return packetSendRatePerMillisecond;
  }

  public double getPacketReceiptRatePerMillisecond() {
    return packetReceiptRatePerMillisecond;
  }

  public double getMeanPacketSizeInBytes() {
    return meanPacketSizeInBytes;
  }

  public double getBytesReceivedByServerDuringMiddleQuantiles() {
    return bytesReceivedByServerDuringMiddleQuantiles;
  }

  public double getByteRateDuringMiddleQuantiles() {
    return byteRateDuringMiddleQuantiles;
  }
}
