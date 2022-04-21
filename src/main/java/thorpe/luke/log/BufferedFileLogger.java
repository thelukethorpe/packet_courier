package thorpe.luke.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class BufferedFileLogger implements Logger {
  private final List<String> buffer = new LinkedList<>();
  private final PrintWriter fileWriter;

  public BufferedFileLogger(File file) throws IOException {
    this.fileWriter = new PrintWriter(new FileWriter(file));
  }

  @Override
  public synchronized void log(String message) {
    buffer.add(message);
  }

  @Override
  public synchronized void flush() {
    buffer.forEach(fileWriter::println);
    buffer.clear();
    fileWriter.flush();
  }

  @Override
  public synchronized void close() {
    fileWriter.close();
  }
}
