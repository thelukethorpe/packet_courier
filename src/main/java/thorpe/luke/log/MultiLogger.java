package thorpe.luke.log;

import java.util.Collection;

public class MultiLogger implements Logger {
  private final Collection<Logger> loggers;

  public MultiLogger(Collection<Logger> loggers) {
    this.loggers = loggers;
  }

  @Override
  public void log(String message) {
    loggers.forEach(logger -> logger.log(message));
  }

  @Override
  public void flush() {
    loggers.forEach(Logger::flush);
  }

  @Override
  public void close() {
    loggers.forEach(Logger::close);
  }
}
