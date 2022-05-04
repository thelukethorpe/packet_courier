package thorpe.luke.log;

import java.io.PrintStream;

public class ConsoleLogger implements Logger {

  private static final ConsoleLogger OUT = new ConsoleLogger(System.out);
  private static final ConsoleLogger ERR = new ConsoleLogger(System.err);

  private final PrintStream printStream;

  private ConsoleLogger(PrintStream printStream) {
    this.printStream = printStream;
  }

  public static ConsoleLogger out() {
    return OUT;
  }

  public static ConsoleLogger err() {
    return ERR;
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
