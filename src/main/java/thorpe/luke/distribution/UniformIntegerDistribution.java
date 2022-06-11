package thorpe.luke.distribution;

import java.util.Random;

public class UniformIntegerDistribution implements Distribution<Integer> {

  private final int min;
  private final int max;

  public UniformIntegerDistribution(int min, int max) {
    assert (min < max);
    this.min = min;
    this.max = max;
  }

  @Override
  public Integer sample(Random random) {
    return random.nextInt(max - min) + min;
  }

  @Override
  public Double mean() {
    // Double is used to prevent integer overflow.
    double range = (double) (max - 1) + (double) min;
    return range / 2.0;
  }

  @Override
  public Double variance() {
    // Double is used to prevent integer overflow.
    double difference = max - min - 1;
    return difference * difference / 12.0;
  }
}
