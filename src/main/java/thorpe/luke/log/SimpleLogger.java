package thorpe.luke.log;

import java.util.function.Consumer;

public class SimpleLogger implements Logger {
  private final Consumer<String> consumer;

  public SimpleLogger(Consumer<String> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void log(String message) {
    consumer.accept(message);
  }

  @Override
  public void flush() {}

  @Override
  public void close() {}
}
