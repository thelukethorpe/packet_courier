package thorpe.luke.distribution;

import java.util.Random;

public class UniformIntegerDistribution implements Distribution<Integer> {

  private final UniformRealDistribution uniformRealDistribution;

  public UniformIntegerDistribution(int min, int max) {
    this.uniformRealDistribution = new UniformRealDistribution(min, max);
  }

  @Override
  public Integer sample(Random random) {
    return (int) Math.round(uniformRealDistribution.sample(random));
  }

  @Override
  public Double mean() {
    return uniformRealDistribution.mean();
  }

  @Override
  public Double variance() {
    return uniformRealDistribution.variance();
  }
}
