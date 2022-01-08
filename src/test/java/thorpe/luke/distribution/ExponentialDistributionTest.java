package thorpe.luke.distribution;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExponentialDistributionTest extends DistributionTest<Double> {

  @Override
  protected Collection<DistributionWithCdfTable> getSomeDistributionsWithCdfTables() {
    return Stream.of(0.01, 0.1, 0.25, 0.33, 0.42, 0.5, 0.75, 1.0, 5.0, 10.0, 25.0, 42.0, 50.0)
        .map(this::distributionWithCdfTable)
        .collect(Collectors.toList());
  }

  private static final int CDF_TABLE_SIZE = 21;

  private DistributionWithCdfTable distributionWithCdfTable(double lambda) {
    Map<Double, Double> cdfTable = new HashMap<>();
    for (int i = 0; i < CDF_TABLE_SIZE; i++) {
      double percentile = i / (double) (CDF_TABLE_SIZE - 1);
      cdfTable.put(percentile, -Math.log(1.0 - percentile) / lambda);
    }
    return new DistributionWithCdfTable(new ExponentialDistribution(lambda), cdfTable);
  }
}
