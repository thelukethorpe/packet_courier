package thorpe.luke.log;

public interface Logger {
  void log(String message);

  void flush();

  void close();
}
