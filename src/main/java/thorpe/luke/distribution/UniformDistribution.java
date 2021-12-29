package thorpe.luke.distribution;

import java.util.Random;

public class UniformDistribution implements Distribution<Double> {

  private final double min;
  private final double max;

  public UniformDistribution(double min, double max) {
    assert (min <= max);
    this.min = min;
    this.max = max;
  }

  @Override
  public Double sample(Random random) {
    return (max - min) * random.nextDouble() + min;
  }

  @Override
  public Double mean() {
    return (min + max) / 2;
  }

  @Override
  public Double variance() {
    return (max - min) * (max - min) / 12;
  }
}
