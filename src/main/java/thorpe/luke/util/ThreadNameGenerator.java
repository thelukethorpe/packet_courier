package thorpe.luke.util;

public class ThreadNameGenerator {

  public static String generateThreadName(String prefix) {
    return String.format("[%s]-Thread", prefix);
  }
}
