package thorpe.luke.distribution;

import java.util.Random;

public class NormalDistribution implements Distribution<Double> {

  private final UniformRealDistribution uniformRealDistribution =
      new UniformRealDistribution(0.0, 1.0);
  private final double mean;
  private final double standardDeviation;
  private Double nextSample;

  public NormalDistribution(double mean, double standardDeviation) {
    assert (standardDeviation > 0.0);
    this.mean = mean;
    this.standardDeviation = standardDeviation;
  }

  @Override
  public Double sample(Random random) {
    if (nextSample != null) {
      double sample = nextSample;
      nextSample = null;
      return sample;
    }
    // Uses Box-Muller transform.
    double u1 = uniformRealDistribution.sample(random);
    double u2 = uniformRealDistribution.sample(random);
    double r = Math.sqrt(-2.0 * Math.log(u1));
    double theta = 2.0 * Math.PI * u2;
    double z0 = r * Math.cos(theta);
    double z1 = r * Math.sin(theta);
    nextSample = mean + standardDeviation * z0;
    return mean + standardDeviation * z1;
  }

  @Override
  public Double mean() {
    return mean;
  }

  @Override
  public Double variance() {
    return standardDeviation * standardDeviation;
  }
}
