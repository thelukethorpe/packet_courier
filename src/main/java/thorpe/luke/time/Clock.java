package thorpe.luke.time;

import java.time.LocalDateTime;

@FunctionalInterface
public interface Clock {
  LocalDateTime now();
}
