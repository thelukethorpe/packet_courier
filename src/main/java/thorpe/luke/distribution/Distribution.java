package thorpe.luke.distribution;

public interface Distribution<T> {
  T sample();

  Double mean();

  Double variance();
}
