package thorpe.luke.network.simulation.analysis.aggregator;

public class AnalysisMessageParseException extends Exception {
  public AnalysisMessageParseException(Exception e) {
    super(e);
  }

  public AnalysisMessageParseException(String message) {
    super(message);
  }

  public AnalysisMessageParseException() {}
}
