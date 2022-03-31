package thorpe.luke.log;

public class ConsoleLogger implements Logger {

  @Override
  public synchronized void log(String message) {
    System.out.println(message);
  }

  @Override
  public synchronized void flush() {
    System.out.flush();
  }

  @Override
  public synchronized void close() {
    // Do nothing.
  }
}
