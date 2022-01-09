package thorpe.luke.distribution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UniformIntegerDistributionTest extends DistributionTest<Integer> {

  @Override
  protected Collection<DistributionWithCdfTable> getSomeDistributionsWithCdfTables() {
    return Arrays.asList(
        distributionWithCdfTable(0, Integer.MAX_VALUE),
        distributionWithCdfTable(42, 1337),
        distributionWithCdfTable(0, 100),
        distributionWithCdfTable(-69, 420));
  }

  private static final int CDF_TABLE_SIZE = 21;

  private DistributionWithCdfTable distributionWithCdfTable(int min, int max) {
    Map<Double, Integer> cdfTable = new HashMap<>();
    for (int i = 0; i < CDF_TABLE_SIZE; i++) {
      double percentile = i / (double) (CDF_TABLE_SIZE - 1);
      cdfTable.put(percentile, (int) Math.round(min + percentile * (max - min)));
    }
    return new DistributionWithCdfTable(new UniformIntegerDistribution(min, max), cdfTable);
  }
}
