package thorpe.luke.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;

public abstract class DistributionTest<T> {

  protected abstract Collection<DistributionWithCdfTable> getSomeDistributionsWithCdfTables();

  private final Collection<DistributionWithCdfTable> someDistributionsWithCdfTables =
      getSomeDistributionsWithCdfTables();

  protected void forManySeeds(BiConsumer<DistributionWithCdfTable, Random> test) {
    Stream.of(0, 1, 42, 100, 200, 1337)
        .map(Random::new)
        .forEach(
            random ->
                someDistributionsWithCdfTables.forEach(
                    distributedWithCdfTable -> test.accept(distributedWithCdfTable, random)));
  }

  protected double toDouble(T value) {
    if (value instanceof Boolean) {
      return (Boolean) value ? 1.0 : 0.0;
    } else if (value instanceof Integer) {
      return ((Integer) value).doubleValue();
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
        (distributionWithCdfTable, random) -> {
          Distribution<T> distribution = distributionWithCdfTable.distribution;
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

  @Test
  public void testCumulativeDistributionValuesAreApproximatelyCorrect() {
    // CDF percentiles obtained via a monte-carlo sampling from distributions are within a
    // reasonable delta of derived values.
    final double DELTA = 0.01;
    final int N = 50_000;
    forManySeeds(
        (distributionWithCdfTable, random) -> {
          Distribution<T> distribution = distributionWithCdfTable.distribution;
          List<Double> samples =
              IntStream.range(0, N)
                  .mapToObj(i -> distribution.sample(random))
                  .map(this::toDouble)
                  .sorted()
                  .collect(Collectors.toList());
          for (Map.Entry<Double, T> cdfTableEntry : distributionWithCdfTable.cdfTable.entrySet()) {
            double truePercentile = cdfTableEntry.getKey();
            double cdfValue = toDouble(cdfTableEntry.getValue());
            double samplePercentile =
                samples.stream().filter(sample -> sample <= cdfValue).count()
                    / (double) samples.size();
            assertThat(truePercentile).isCloseTo(samplePercentile, within(DELTA));
          }
        });
  }

  protected class DistributionWithCdfTable {
    private final Distribution<T> distribution;
    private final Map<Double, T> cdfTable;

    public DistributionWithCdfTable(Distribution<T> distribution, Map<Double, T> cdfTable) {
      this.distribution = distribution;
      this.cdfTable = cdfTable;
    }
  }
}
