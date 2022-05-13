package thorpe.luke.network.simulation.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class WorkerProcessExitStatus {
  private final List<String> errors;

  private WorkerProcessExitStatus(List<String> errors) {
    this.errors = errors;
  }

  public static WorkerProcessExitStatus success() {
    return new WorkerProcessExitStatus(Collections.emptyList());
  }

  public static WorkerProcessExitStatus failure(InputStream errorStream) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream));
    List<String> errors = new LinkedList<>();
    String error = bufferedReader.readLine();
    while (error != null) {
      errors.add(error);
      error = bufferedReader.readLine();
    }
    return new WorkerProcessExitStatus(errors);
  }

  public boolean isSuccess() {
    return errors.isEmpty();
  }

  public List<String> getErrors() {
    return errors;
  }
}
