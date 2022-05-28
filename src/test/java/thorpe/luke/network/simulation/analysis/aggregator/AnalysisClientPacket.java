package thorpe.luke.network.simulation.analysis.aggregator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AnalysisClientPacket {

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
  public static final String PREFIX = "!";
  private static final String DELIMITER = "~";
  private static final int NUMBER_OF_DATA_ELEMENTS = 6;

  private final String clientName;
  private final int packetId;
  private final LocalDateTime dateTimeOfSending;

  public AnalysisClientPacket(String clientName, int packetId, LocalDateTime dateTimeOfSending) {
    this.clientName = clientName;
    this.packetId = packetId;
    this.dateTimeOfSending = dateTimeOfSending;
  }

  public static AnalysisClientPacket parse(String logLine) throws AnalysisMessageParseException {
    String[] dataElements = logLine.split(DELIMITER);
    if (dataElements.length != NUMBER_OF_DATA_ELEMENTS) {
      throw new AnalysisMessageParseException(
          String.format(
              "Log line \"%s\" contains %d elements but should have %d.",
              logLine, dataElements.length, NUMBER_OF_DATA_ELEMENTS));
    } else if (!dataElements[0].equals(PREFIX)) {
      throw new AnalysisMessageParseException(
          String.format("Log line \"%s\" does not start with \"%s\".", logLine, PREFIX));
    }
    try {
      String clientName = dataElements[1];
      int packetId = Integer.parseInt(dataElements[2]);
      LocalDateTime dateTimeOfSending = LocalDateTime.parse(dataElements[3], DATE_TIME_FORMATTER);
      return new AnalysisClientPacket(clientName, packetId, dateTimeOfSending);
    } catch (Exception e) {
      throw new AnalysisMessageParseException(e);
    }
  }

  public UniquePacketId getUniquePacketId() {
    return new UniquePacketId(packetId, clientName);
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
}
