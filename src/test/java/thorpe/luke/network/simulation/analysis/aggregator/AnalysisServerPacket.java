package thorpe.luke.network.simulation.analysis.aggregator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AnalysisServerPacket {

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
  public static final String PREFIX = "?";
  private static final String DELIMITER = "~";
  private static final int NUMBER_OF_DATA_ELEMENTS = 8;

  private final String serverName;
  private final LocalDateTime dateTimeOfReceipt;
  private final String clientName;
  private final int packetId;
  private final LocalDateTime dateTimeOfSending;
  private final boolean isChecksumCorrupted;

  public AnalysisServerPacket(
      String serverName,
      LocalDateTime dateTimeOfReceipt,
      String clientName,
      int packetId,
      LocalDateTime dateTimeOfSending,
      boolean isChecksumCorrupted) {
    this.serverName = serverName;
    this.dateTimeOfReceipt = dateTimeOfReceipt;
    this.clientName = clientName;
    this.packetId = packetId;
    this.dateTimeOfSending = dateTimeOfSending;
    this.isChecksumCorrupted = isChecksumCorrupted;
  }

  public static AnalysisServerPacket parse(String logLine) throws AnalysisMessageParseException {
    String[] dataElements = logLine.split(DELIMITER);
    if (dataElements.length != NUMBER_OF_DATA_ELEMENTS) {
      throw new AnalysisMessageParseException(
          String.format(
              "Log line \"%s\" contains %d elements but should have %d.",
              logLine, dataElements.length, NUMBER_OF_DATA_ELEMENTS));
    } else if (!dataElements[0].equals(PREFIX)) {
      throw new AnalysisMessageParseException(
          String.format("Log line \"%s\" does not start with \"%s\".", logLine, PREFIX));
    } else if (!dataElements[3].equals(AnalysisClientPacket.PREFIX)) {
      throw new AnalysisMessageParseException(
          String.format(
              "Log line \"%s\" does have the client prefix, \"%s\", as its fourth element.",
              logLine, AnalysisClientPacket.PREFIX));
    }
    try {
      String serverName = dataElements[1];
      LocalDateTime dateTimeOfReceipt = LocalDateTime.parse(dataElements[2], DATE_TIME_FORMATTER);
      String clientName = dataElements[4];
      int packetId = Integer.parseInt(dataElements[5]);
      LocalDateTime dateTimeOfSending =
          LocalDateTime.parse(dataElements[6], AnalysisClientPacket.DATE_TIME_FORMATTER);
      boolean isCorrupted = Boolean.parseBoolean(dataElements[7]);
      return new AnalysisServerPacket(
          serverName, dateTimeOfReceipt, clientName, packetId, dateTimeOfSending, isCorrupted);
    } catch (Exception e) {
      throw new AnalysisMessageParseException(e);
    }
  }

  public UniquePacketId getUniquePacketId() {
    return new UniquePacketId(packetId, clientName);
  }

  public String getServerName() {
    return serverName;
  }

  public LocalDateTime getDateTimeOfReceipt() {
    return dateTimeOfReceipt;
  }

  public String getClientName() {
    return clientName;
  }

  public int getPacketId() {
    return packetId;
  }

  public LocalDateTime getDateTimeOfSending() {
    return dateTimeOfSending;
  }

  public boolean isChecksumCorrupted() {
    return isChecksumCorrupted;
  }
}
