package thorpe.luke.time;

import java.time.LocalDateTime;

public class WallClock implements Clock {
  @Override
  public void tick() {
    // Do nothing.
  }

  @Override
  public LocalDateTime now() {
    return LocalDateTime.now();
  }
}
