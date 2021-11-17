package thorpe.luke.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Collection;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;

public abstract class DistributionTest<T> {

  protected abstract Collection<Distribution<T>> getSomeDistributions();

  protected void forManySeeds(BiConsumer<Distribution<T>, Random> test) {
    Stream.of(0, 1, 42, 100, 200, 1337)
        .map(Random::new)
        .forEach(
            random ->
                getSomeDistributions().forEach(distribution -> test.accept(distribution, random)));
  }

  protected double toDouble(T value) {
    if (value instanceof Boolean) {
      return (Boolean) value ? 1.0 : 0.0;
    } else {
      return (Double) value;
    }
  }

  @Test
  public void testSampleMeanIsApproximatelyEqualToTheoreticalMean() {
    // Confidence intervals with 99% level of certainty.
    final double Z = 2.58;
    final int N = 10_000;
    forManySeeds(
        (distribution, random) -> {
          double sampleMean =
              IntStream.range(0, N)
                  .mapToObj(i -> distribution.sample(random))
                  .mapToDouble(this::toDouble)
                  .average()
                  .orElseThrow(AssertionError::new);
          double confidenceInterval = Z * Math.sqrt(distribution.variance() / N);
          assertThat(distribution.mean()).isCloseTo(sampleMean, within(confidenceInterval));
        });
  }
}
