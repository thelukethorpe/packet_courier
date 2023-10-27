package thorpe.luke.time;

import java.time.LocalDateTime;

public class TickableClock implements Tickable, Clock {
  private LocalDateTime now;

  public TickableClock(LocalDateTime now) {
    this.now = now;
  }

  @Override
  public void tick(LocalDateTime now) {
    this.now = now;
  }

  @Override
  public LocalDateTime now() {
    return now;
  }
}
