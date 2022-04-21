package thorpe.luke.log;

import java.io.PrintStream;

public class ConsoleLogger implements Logger {

  private final PrintStream printStream;

  private ConsoleLogger(PrintStream printStream) {
    this.printStream = printStream;
  }

  public static ConsoleLogger out() {
    return new ConsoleLogger(System.out);
  }

  public static ConsoleLogger err() {
    return new ConsoleLogger(System.err);
  }

  @Override
  public synchronized void log(String message) {
    printStream.println(message);
  }

  @Override
  public synchronized void flush() {
    printStream.flush();
  }

  @Override
  public synchronized void close() {
    // Do nothing.
  }
}
