package thorpe.luke.distribution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UniformDistributionTest extends DistributionTest<Double> {

  @Override
  protected Collection<DistributionWithCdfTable> getSomeDistributionsWithCdfTables() {
    return Arrays.asList(
        distributionWithCdfTable(0.0, 1.0),
        distributionWithCdfTable(42.0, 1337.0),
        distributionWithCdfTable(0.0, 100.0),
        distributionWithCdfTable(-69.0, 420.0));
  }

  private static final int CDF_TABLE_SIZE = 21;

  private DistributionWithCdfTable distributionWithCdfTable(double min, double max) {
    Map<Double, Double> cdfTable = new HashMap<>();
    for (int i = 0; i < CDF_TABLE_SIZE; i++) {
      double percentile = i / (double) (CDF_TABLE_SIZE - 1);
      cdfTable.put(percentile, min + percentile * (max - min));
    }
    return new DistributionWithCdfTable(new UniformDistribution(min, max), cdfTable);
  }
}
