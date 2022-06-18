package thorpe.luke.network.simulation.analysis.tabulator;

import java.text.DecimalFormat;
import java.util.*;
import thorpe.luke.network.simulation.analysis.aggregator.AggregatedStatistics;

public class AnalysisTabulator {
  private static final String[] ROW_HEADERS =
      new String[] {
        "Machine",
        "Number of clients",
        "Duration (s)",
        "Packets sent by clients",
        "Packets received by server",
        "Number of unexpected arrivals",
        "Number of missing arrivals",
        "Number of unidentifiable arrivals",
        "Packets with checksum corruption",
        "Junk packets",
        "Mean packet size (bytes)",
        "Mean packet latency (ms)",
        "Mean bandwidth (bytes/ms)",
        "Estimated corruption rate (\\%%)",
        "Packet drop rate (\\%%)",
        "Mean number of packet duplications",
        "Mean packet rate per client (packets/s)",
        "Mean bandwidth per client (bytes/s)"
      };

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

  private final String name;
  private final Map<String, Data> machineToDataMap = new TreeMap<>();

  public AnalysisTabulator(String name) {
    this.name = name;
  }

  public void addColumn(
      String machine, Integer numberOfClients, AggregatedStatistics aggregatedStatistics) {
    machineToDataMap.putIfAbsent(machine, new Data());
    machineToDataMap.get(machine).addColumn(numberOfClients, aggregatedStatistics);
  }

  public String tabulate() {
    List<String> tableRows = new LinkedList<>();
    tableRows.add("\\begin{table}[!h]");
    tableRows.add("\\centering");
    tableRows.add("\\resizebox{\\textwidth}{!}{");

    String[][] table = new String[ROW_HEADERS.length][];
    int totalNumberOfColumns = 1;
    {
      StringBuilder beginTabular = new StringBuilder("\\begin{tabular}{|l|");
      for (Data data : machineToDataMap.values()) {
        int numberOfColumns = data.getNumberOfClientsRow().size();
        beginTabular.append(String.join("", Collections.nCopies(numberOfColumns, "c")));
        beginTabular.append("|");
        totalNumberOfColumns += numberOfColumns;
      }
      beginTabular.append("}");
      tableRows.add(beginTabular.toString());
    }

    tableRows.add("\\hline");

    for (int i = 0; i < table.length; i++) {
      table[i] = new String[totalNumberOfColumns];
      table[i][0] = ROW_HEADERS[i];
    }

    int columnIndex = 1;
    for (Map.Entry<String, Data> machineToDataEntry : machineToDataMap.entrySet()) {
      String machine = machineToDataEntry.getKey();
      Data data = machineToDataEntry.getValue();
      int numberOfColumns = data.getNumberOfClientsRow().size();
      int machineIndex = columnIndex + numberOfColumns / 2;
      table[0][machineIndex] = machine;
      for (Map.Entry<Integer, AggregatedStatistics> numberOfClientsToAggregatedStatisticsEntry :
          data.numberOfClientsToAggregatedStatisticsMap.entrySet()) {
        int numberOfClients = numberOfClientsToAggregatedStatisticsEntry.getKey();
        AggregatedStatistics aggregatedStatistics =
            numberOfClientsToAggregatedStatisticsEntry.getValue();
        table[1][columnIndex] = String.valueOf(numberOfClients);
        double durationInSeconds = aggregatedStatistics.getDurationInMilliseconds() / 1_000.0;
        table[2][columnIndex] = String.valueOf(durationInSeconds);
        table[3][columnIndex] =
            String.valueOf(aggregatedStatistics.getNumberOfPacketsSentByClients());
        table[4][columnIndex] =
            String.valueOf(aggregatedStatistics.getNumberOfPacketsReceivedByServer());
        table[5][columnIndex] =
            String.valueOf(aggregatedStatistics.getNumberOfUnexpectedArrivals());
        table[6][columnIndex] = String.valueOf(aggregatedStatistics.getNumberOfMissingArrivals());
        table[7][columnIndex] =
            String.valueOf(aggregatedStatistics.getNumberOfUnidentifiableArrivals());
        table[8][columnIndex] =
            String.valueOf(aggregatedStatistics.getNumberOfChecksumCorruptedPackets());
        table[9][columnIndex] = String.valueOf(aggregatedStatistics.getNumberOfJunkPackets());
        table[10][columnIndex] =
            String.valueOf(DECIMAL_FORMAT.format(aggregatedStatistics.getMeanPacketSizeInBytes()));
        table[11][columnIndex] =
            String.valueOf(
                DECIMAL_FORMAT.format(aggregatedStatistics.getMeanPacketLatencyInMilliseconds()));
        table[12][columnIndex] =
            String.valueOf(
                DECIMAL_FORMAT.format(aggregatedStatistics.getByteRateDuringMiddleQuantiles()));

        double estimatedCorruptionRate =
            100.0
                * (aggregatedStatistics.getNumberOfChecksumCorruptedPackets()
                    + aggregatedStatistics.getNumberOfJunkPackets()
                    + aggregatedStatistics.getNumberOfPacketsSentByClients()
                    - aggregatedStatistics.getNumberOfPacketsReceivedByServer())
                / (double) aggregatedStatistics.getNumberOfPacketsSentByClients();
        table[13][columnIndex] = String.valueOf(DECIMAL_FORMAT.format(estimatedCorruptionRate));
        double packetDropRate = 100.0 * aggregatedStatistics.getMissingArrivalRate();
        table[14][columnIndex] = String.valueOf(DECIMAL_FORMAT.format(packetDropRate));
        double meanNumberOfPacketDuplications =
            aggregatedStatistics.getMeanUnexpectedArrivalsPerSend();
        table[15][columnIndex] =
            String.valueOf(DECIMAL_FORMAT.format(meanNumberOfPacketDuplications));
        double meanPacketRatePerClient =
            1000.0
                * aggregatedStatistics.getPacketReceiptRatePerMillisecond()
                / (double) numberOfClients;
        table[16][columnIndex] = String.valueOf(DECIMAL_FORMAT.format(meanPacketRatePerClient));
        double meanBandwidthPerClient =
            1000.0
                * aggregatedStatistics.getByteRateDuringMiddleQuantiles()
                / (double) numberOfClients;
        table[17][columnIndex] = String.valueOf(DECIMAL_FORMAT.format(meanBandwidthPerClient));
        columnIndex++;
      }
    }
    {
      StringBuilder row = new StringBuilder("\\textbf{Machine}");
      for (int j = 1; j < table[0].length; j++) {
        row.append(" & ");
        if (table[0][j] != null) {
          row.append(table[0][j]);
        }
      }
      row.append("\\\\ \\hline");
      tableRows.add(row.toString());
    }

    for (int i = 1; i < table.length; i++) {
      StringBuilder row = new StringBuilder("\\textbf{");
      row.append(table[i][0]);
      row.append("}");
      for (int j = 1; j < table[i].length; j++) {
        row.append(" & ");
        row.append("\\multicolumn{1}{c|}{");
        row.append(table[i][j]);
        row.append("}");
      }
      row.append("\\\\ \\hline");
      tableRows.add(row.toString());
    }

    tableRows.add("\\end{tabular}");
    tableRows.add("}");
    tableRows.add("\\caption{" + name + "}");
    tableRows.add("\\label{table:analysis_results_" + name + "}");
    tableRows.add("\\end{table}");

    return String.join(System.lineSeparator(), tableRows);
  }

  private static class Data {
    private final Map<Integer, AggregatedStatistics> numberOfClientsToAggregatedStatisticsMap =
        new TreeMap<>();

    public List<Integer> getNumberOfClientsRow() {
      return new ArrayList<>(numberOfClientsToAggregatedStatisticsMap.keySet());
    }

    public void addColumn(Integer numberOfClients, AggregatedStatistics aggregatedStatistics) {
      numberOfClientsToAggregatedStatisticsMap.put(numberOfClients, aggregatedStatistics);
    }
  }
}
