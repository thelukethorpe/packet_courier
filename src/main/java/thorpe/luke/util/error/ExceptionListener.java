package thorpe.luke.util.error;

@FunctionalInterface
public interface ExceptionListener {
  void invoke(Exception exception);
}
