package thorpe.luke.distribution;

import java.util.Random;

public class UniformDistribution implements Distribution<Double> {

  private final double min;
  private final double max;
  private final Random random;

  public UniformDistribution(double min, double max, Random random) {
    assert (min <= max);
    this.min = min;
    this.max = max;
    this.random = random;
  }

  @Override
  public Double sample() {
    return (max - min) * random.nextDouble() + min;
  }
}
