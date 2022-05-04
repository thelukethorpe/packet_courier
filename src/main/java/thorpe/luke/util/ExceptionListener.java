package thorpe.luke.util;

@FunctionalInterface
public interface ExceptionListener {
  void invoke(Exception exception);
}
