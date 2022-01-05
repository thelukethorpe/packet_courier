package thorpe.luke.util;

public class Mutable<T> {
  private T value;

  public Mutable(T value) {
    this.value = value;
  }

  public T get() {
    return value;
  }

  public void set(T value) {
    this.value = value;
  }
}
