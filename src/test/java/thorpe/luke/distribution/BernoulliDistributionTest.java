package thorpe.luke.distribution;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BernoulliDistributionTest extends DistributionTest<Boolean> {

  @Override
  protected Collection<Distribution<Boolean>> getSomeDistributions() {
    return Stream.of(0.0, 0.01, 0.1, 0.2, 0.25, 0.33, 0.42, 0.5, 0.6, 0.75, 0.99, 1.0)
        .map(BernoulliDistribution::new)
        .collect(Collectors.toList());
  }
}
