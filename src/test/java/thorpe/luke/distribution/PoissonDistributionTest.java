package thorpe.luke.distribution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PoissonDistributionTest extends DistributionTest<Integer> {

  @Override
  protected Collection<DistributionWithCdfTable> getSomeDistributionsWithCdfTables() {
    return Arrays.asList(
        distributionWithCdfTable(
            0.1,
            new HashMap<Double, Integer>() {
              {
                put(0.9048, 0);
                put(0.9953, 1);
                put(0.9998, 2);
                put(1.0000, 3);
              }
            }),
        distributionWithCdfTable(
            0.5,
            new HashMap<Double, Integer>() {
              {
                put(0.6065, 0);
                put(0.9098, 1);
                put(0.9856, 2);
                put(0.9982, 3);
                put(0.9998, 4);
                put(1.0000, 5);
              }
            }),
        distributionWithCdfTable(
            0.9,
            new HashMap<Double, Integer>() {
              {
                put(0.4066, 0);
                put(0.7725, 1);
                put(0.9371, 2);
                put(0.9865, 3);
                put(0.9977, 4);
                put(0.9997, 5);
                put(1.0000, 6);
              }
            }),
        distributionWithCdfTable(
            1.0,
            new HashMap<Double, Integer>() {
              {
                put(0.3679, 0);
                put(0.7358, 1);
                put(0.9197, 2);
                put(0.9810, 3);
                put(0.9963, 4);
                put(0.9994, 5);
                put(0.9999, 6);
                put(1.0000, 7);
              }
            }),
        distributionWithCdfTable(
            1.8,
            new HashMap<Double, Integer>() {
              {
                put(0.1653, 0);
                put(0.4628, 1);
                put(0.7306, 2);
                put(0.8913, 3);
                put(0.9636, 4);
                put(0.9896, 5);
                put(0.9974, 6);
                put(0.9994, 7);
                put(0.9999, 8);
                put(1.0000, 9);
              }
            }),
        distributionWithCdfTable(
            4.5,
            new HashMap<Double, Integer>() {
              {
                put(0.0111, 0);
                put(0.0611, 1);
                put(0.1736, 2);
                put(0.3423, 3);
                put(0.5321, 4);
                put(0.7029, 5);
                put(0.8311, 6);
                put(0.9134, 7);
                put(0.9597, 8);
                put(0.9829, 9);
                put(0.9933, 10);
                put(0.9976, 11);
                put(0.9992, 12);
                put(0.9997, 13);
                put(0.9999, 14);
                put(1.0000, 15);
              }
            }));
  }

  private DistributionWithCdfTable distributionWithCdfTable(
      double lambda, Map<Double, Integer> cdfTable) {
    return new DistributionWithCdfTable(new PoissonDistribution(lambda), cdfTable);
  }
}
