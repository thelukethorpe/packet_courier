package thorpe.luke.distribution;

import java.util.Random;

public class ExponentialDistribution implements Distribution<Double> {

  private final double lambda;

  public ExponentialDistribution(double lambda) {
    assert (lambda > 0.0);
    this.lambda = lambda;
  }

  @Override
  public Double sample(Random random) {
    return -Math.log(1.0 - random.nextDouble()) / lambda;
  }

  @Override
  public Double mean() {
    return 1.0 / lambda;
  }

  @Override
  public Double variance() {
    return 1.0 / (lambda * lambda);
  }
}
