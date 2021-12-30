package thorpe.luke.time;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public class VirtualClock implements Clock {
  private final ChronoUnit timeUnit;
  private LocalDateTime now = LocalDateTime.of(0, Month.JANUARY, 1, 0, 0, 0, 0);

  public VirtualClock(ChronoUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  @Override
  public void tick() {
    now = now.plus(Duration.of(1, timeUnit));
  }

  @Override
  public LocalDateTime now() {
    return now;
  }
}
