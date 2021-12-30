package thorpe.luke.time;

import java.time.LocalDateTime;

public interface Clock {
  void tick();

  LocalDateTime now();
}
