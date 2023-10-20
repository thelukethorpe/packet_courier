package thorpe.luke.util.error;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.function.Function;
import org.junit.Test;

public class ErrorCollectorTest {

  private static final String ERROR_PREFIX = "Some prefix";
  private static final String ERROR_MESSAGE_1 = "Uh oh, an error occurred!";
  private static final String ERROR_MESSAGE_2 = "500 something went wrong";

  private static class TestException extends Exception {
    public TestException(String message) {
      super(message);
    }
  }

  @Test
  public void testEmptyErrorCollectorDoesNotThrowException() {
    assertThatNoException()
        .isThrownBy(
            () -> new ErrorCollector().ifErrorsThrowException(ERROR_PREFIX, TestException::new));
  }

  @Test
  public void testNonEmptyErrorCollectorThrowsException() {
    ErrorCollector errorCollector = new ErrorCollector();
    errorCollector.add(ERROR_MESSAGE_1);
    assertThatThrownBy(
            () ->
                errorCollector.ifErrorsThrowException(
                    ERROR_PREFIX, (Function<String, Exception>) TestException::new))
        .isInstanceOf(TestException.class);
  }

  @Test
  public void testErrorCollectorAggregatesErrorsElegantly() {
    ErrorCollector errorCollector = new ErrorCollector();
    errorCollector.add(ERROR_MESSAGE_1);
    errorCollector.add(ERROR_MESSAGE_2);
    assertThatThrownBy(
            () -> errorCollector.ifErrorsThrowException(ERROR_PREFIX, TestException::new))
        .hasMessage(ERROR_PREFIX + ":\n * " + ERROR_MESSAGE_1 + "\n * " + ERROR_MESSAGE_2);
  }
}
