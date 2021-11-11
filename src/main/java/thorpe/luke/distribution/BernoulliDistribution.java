package thorpe.luke.distribution;

import java.util.Random;

public class BernoulliDistribution implements Distribution<Boolean> {

  private final double probability;
  private final Random random;

  public BernoulliDistribution(double probability, Random random) {
    assert (0.0 <= probability && probability <= 1.0);
    this.probability = probability;
    this.random = random;
  }

  @Override
  public Boolean sample() {
    return random.nextDouble() < probability;
  }
}
