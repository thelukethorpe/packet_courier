package thorpe.luke.distribution;

import java.util.Random;

public class BernoulliDistribution implements Distribution<Boolean> {

  private final double probability;

  public BernoulliDistribution(double probability) {
    assert (0.0 <= probability && probability <= 1.0);
    this.probability = probability;
  }

  @Override
  public Boolean sample(Random random) {
    return random.nextDouble() < probability;
  }

  @Override
  public Double mean() {
    return probability;
  }

  @Override
  public Double variance() {
    return probability * (1.0 - probability);
  }
}
