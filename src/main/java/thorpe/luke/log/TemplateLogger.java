package thorpe.luke.log;

import java.util.function.Function;

public class TemplateLogger implements Logger {
  private final Function<String, String> template;
  private final Logger logger;

  public TemplateLogger(Function<String, String> template, Logger logger) {
    this.template = template;
    this.logger = logger;
  }

  @Override
  public void log(String message) {
    logger.log(template.apply(message));
  }

  @Override
  public void flush() {
    logger.flush();
  }

  @Override
  public void close() {
    logger.close();
  }
}
