package thorpe.luke.time;

import java.time.LocalDateTime;

@FunctionalInterface
public interface Tickable {
  void tick(LocalDateTime now);
}
