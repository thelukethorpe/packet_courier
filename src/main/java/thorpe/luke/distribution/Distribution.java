package thorpe.luke.distribution;

import java.util.Random;

public interface Distribution<T> {
  T sample(Random random);

  Double mean();

  Double variance();
}
