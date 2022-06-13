package thorpe.luke.util;

public class GarbageCollector {
  private final Prunable prunable;
  private final int period;
  private int time = 0;

  public GarbageCollector(Prunable prunable, int period) {
    this.prunable = prunable;
    this.period = period;
  }

  private synchronized boolean incrementAndCompareTimeWithPeriod() {
    if (period <= ++time) {
      time = 0;
      return true;
    }
    return false;
  }

  public void tick() {
    boolean periodComplete = incrementAndCompareTimeWithPeriod();
    if (periodComplete) {
      prunable.prune();
    }
  }
}
