package thorpe.luke.distribution;

import java.util.Random;
import java.util.TreeMap;

public class PoissonDistribution implements Distribution<Integer> {

  private final double lambda;
  private final TreeMap<Double, Integer> cdfTable;

  public PoissonDistribution(double lambda) {
    this.lambda = lambda;
    this.cdfTable = new TreeMap<>();
    for (CdfTableBuilder cdfTableBuilder = new CdfTableBuilder(lambda);
        !cdfTableBuilder.isDone();
        cdfTableBuilder.next()) {
      cdfTable.put(cdfTableBuilder.getPercentile(), cdfTableBuilder.getX());
    }
  }

  private static class CdfTableBuilder {
    private final double lambda;
    private double term;
    private double percentile;
    private int x;

    private CdfTableBuilder(double lambda) {
      this.lambda = lambda;
      this.term = Math.exp(-lambda);
      this.percentile = 0.0;
      this.x = 0;
    }

    public boolean isDone() {
      return percentile >= 1.0;
    }

    public void next() {
      x++;
      percentile = percentile + term > percentile ? percentile + term : 1.0;
      term *= lambda / (double) x;
    }

    public double getPercentile() {
      return percentile;
    }

    public int getX() {
      return x;
    }
  }

  @Override
  public Integer sample(Random random) {
    return cdfTable.floorEntry(random.nextDouble()).getValue();
  }

  @Override
  public Double mean() {
    return lambda;
  }

  @Override
  public Double variance() {
    return lambda;
  }
}
