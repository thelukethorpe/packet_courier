package thorpe.luke.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class GarbageCollectorTest {

  @Test
  public void testTick() {
    Mutable<Integer> counter = new Mutable<>(0);
    Prunable countingPrunable = () -> counter.set(counter.get() + 1);
    GarbageCollector garbageCollector = new GarbageCollector(countingPrunable, 3);
    garbageCollector.tick();
    assertThat(counter.get()).isEqualTo(0);
    garbageCollector.tick();
    assertThat(counter.get()).isEqualTo(0);
    garbageCollector.tick();
    assertThat(counter.get()).isEqualTo(1);
    garbageCollector.tick();
    assertThat(counter.get()).isEqualTo(1);
    garbageCollector.tick();
    assertThat(counter.get()).isEqualTo(1);
    garbageCollector.tick();
    assertThat(counter.get()).isEqualTo(2);
    garbageCollector.tick();
    assertThat(counter.get()).isEqualTo(2);
  }
}
