package thorpe.luke.distribution;

import java.util.Arrays;
import java.util.Collection;

public class UniformDistributionTest extends DistributionTest<Double> {

  @Override
  protected Collection<Distribution<Double>> getSomeDistributions() {
    return Arrays.asList(
        new UniformDistribution(0.0, 1.0),
        new UniformDistribution(42.0, 1337.0),
        new UniformDistribution(0.0, 100.0),
        new UniformDistribution(-69.0, 420.0));
  }
}
