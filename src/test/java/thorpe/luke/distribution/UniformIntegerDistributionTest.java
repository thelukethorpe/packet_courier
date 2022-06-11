package thorpe.luke.distribution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class UniformIntegerDistributionTest extends DistributionTest<Integer> {

  @Override
  protected Collection<DistributionWithCdfTable> getSomeDistributionsWithCdfTables() {
    return Arrays.asList(
        distributionWithCdfTable(0, Integer.MAX_VALUE),
        distributionWithCdfTable(Integer.MAX_VALUE >> 1, Integer.MAX_VALUE),
        distributionWithCdfTable(42, 1337),
        distributionWithCdfTable(0, 100),
        distributionWithCdfTable(-69, 420));
  }

  private static final int MAXIMUM_TABLE_SIZE = 1_000;

  private DistributionWithCdfTable distributionWithCdfTable(int min, int max) {
    Map<Double, Integer> cdfTable = new HashMap<>();
    int step = Math.max(1, (max - min) / MAXIMUM_TABLE_SIZE);
    UnaryOperator<Integer> incrementWithoutOverflow =
        i -> {
          int j = i + step;
          return i < j ? j : max;
        };
    for (int i = min; i < max; i = incrementWithoutOverflow.apply(i)) {
      double percentile = (i + 1 - min) / (double) (max - min);
      cdfTable.put(percentile, i);
    }
    cdfTable.put(1.0, max - 1);
    return new DistributionWithCdfTable(new UniformIntegerDistribution(min, max), cdfTable);
  }
}
