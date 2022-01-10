package thorpe.luke.distribution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class NormalDistributionTest extends DistributionTest<Double> {

  private static final Map<Double, Double> STANDARD_NORMAL_DISTRIBUTION_CDF_TABLE =
      new HashMap<Double, Double>() {
        {
          put(0.00023, -3.5);
          put(0.00135, -3.0);
          put(0.00621, -2.5);
          put(0.02275, -2.0);
          put(0.06681, -1.5);
          put(0.15866, -1.0);
          put(0.30854, -0.5);
          put(0.50000, 0.0);
          put(0.69146, 0.5);
          put(0.84134, 1.0);
          put(0.93319, 1.5);
          put(0.97725, 2.0);
          put(0.99379, 2.5);
          put(0.99865, 3.0);
          put(0.99977, 3.5);
        }
      };

  @Override
  protected Collection<DistributionWithCdfTable> getSomeDistributionsWithCdfTables() {
    return Arrays.asList(
        distributionWithCdfTable(0.0, 1.0),
        distributionWithCdfTable(50.0, 10.0),
        distributionWithCdfTable(-25.0, 2.5),
        distributionWithCdfTable(42.0, 13.37),
        distributionWithCdfTable(0.0, 0.5),
        distributionWithCdfTable(1.0, 2.0),
        distributionWithCdfTable(100.0, 16.0));
  }

  private DistributionWithCdfTable distributionWithCdfTable(double mean, double standardDeviation) {
    return new DistributionWithCdfTable(
        new NormalDistribution(mean, standardDeviation),
        STANDARD_NORMAL_DISTRIBUTION_CDF_TABLE
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, entry -> mean + standardDeviation * entry.getValue())));
  }
}
