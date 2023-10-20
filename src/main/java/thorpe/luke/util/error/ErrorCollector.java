package thorpe.luke.util.error;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ErrorCollector {
  private final List<String> errors = new LinkedList<>();

  public void add(String error) {
    errors.add(error);
  }

  private String aggregateErrors() {
    return errors.stream().map(error -> " * " + error).collect(Collectors.joining("\n"));
  }

  public <TException extends Exception> void ifErrorsThrowException(
      String prefix, Function<String, TException> exceptionFactory) throws TException {
    if (!errors.isEmpty()) {
      throw exceptionFactory.apply(prefix + ":\n" + aggregateErrors());
    }
  }
}
