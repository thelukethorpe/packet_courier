package thorpe.luke.time;

import java.time.LocalDateTime;
import java.util.concurrent.*;

public class AsyncTickable implements Tickable {

  private final ExecutorService executorService;
  private final Tickable tickable;
  private Future<?> currentSubmission;

  public AsyncTickable(ExecutorService executorService, Tickable tickable) {
    this.executorService = executorService;
    this.tickable = tickable;
  }

  @Override
  public void tick(LocalDateTime now) {
    if (currentSubmission != null && currentSubmission.isDone()) {
      currentSubmission = executorService.submit(() -> tickable.tick(now));
    }
  }

}
