package thorpe.luke.network.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.junit.Test;

public class TopologyTest {
  public static final Topology EMPTY_TOPOLOGY = Topology.of(Collections.emptyMap());

  public static final Topology SINGLETON_TOPOLOGY =
      Topology.of(
          new HashMap<String, Collection<String>>() {
            {
              put("a", Collections.emptySet());
            }
          });

  public static final Topology TRIANGLE_TOPOLOGY =
      Topology.of(
          new HashMap<String, Collection<String>>() {
            {
              put("a", Collections.singleton("b"));
              put("b", Collections.singleton("c"));
              put("c", Collections.singleton("a"));
            }
          });

  public static final Topology BI_DIRECTIONAL_TRIANGLE_TOPOLOGY =
      Topology.of(
          new HashMap<String, Collection<String>>() {
            {
              put("a", Arrays.asList("b", "c"));
              put("b", Arrays.asList("a", "c"));
              put("c", Arrays.asList("a", "b"));
            }
          });

  public static final Topology TWO_ISLAND_TRIANGLE_TOPOLOGY =
      Topology.of(
          new HashMap<String, Collection<String>>() {
            {
              put("a", Collections.singleton("b"));
              put("b", Collections.singleton("c"));
              put("c", Collections.singleton("a"));
              put("x", Collections.singleton("y"));
              put("y", Collections.singleton("z"));
              put("z", Collections.singleton("x"));
            }
          });

  public static final Topology STAR_TOPOLOGY =
      Topology.of(
          new HashMap<String, Collection<String>>() {
            {
              put("source", Arrays.asList("a", "b", "c", "d", "e"));
            }
          });

  public static final Topology RING_TOPOLOGY =
      Topology.of(
          new HashMap<String, Collection<String>>() {
            {
              put("a", Collections.singleton("b"));
              put("b", Collections.singleton("c"));
              put("c", Collections.singleton("d"));
              put("d", Collections.singleton("e"));
              put("e", Collections.singleton("a"));
            }
          });

  @Test
  public void testGetNodes() {
    assertThat(EMPTY_TOPOLOGY.getNodes()).isEmpty();
    assertThat(SINGLETON_TOPOLOGY.getNodes()).containsExactlyInAnyOrder("a");
    assertThat(TRIANGLE_TOPOLOGY.getNodes()).containsExactlyInAnyOrder("a", "b", "c");
    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.getNodes())
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.getNodes())
        .containsExactlyInAnyOrder("a", "b", "c", "x", "y", "z");
    assertThat(STAR_TOPOLOGY.getNodes())
        .containsExactlyInAnyOrder("source", "a", "b", "c", "d", "e");
    assertThat(RING_TOPOLOGY.getNodes()).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
  }

  @Test
  public void testUndefinedNodeHasNoNeighbours() {
    assertThat(EMPTY_TOPOLOGY.getNeighboursOf("a")).isEmpty();
    assertThat(EMPTY_TOPOLOGY.getNeighboursOf("b")).isEmpty();
    assertThat(EMPTY_TOPOLOGY.getNeighboursOf("x")).isEmpty();
    assertThat(EMPTY_TOPOLOGY.getNeighboursOf("y")).isEmpty();

    assertThat(SINGLETON_TOPOLOGY.getNeighboursOf("b")).isEmpty();
    assertThat(SINGLETON_TOPOLOGY.getNeighboursOf("y")).isEmpty();

    assertThat(TRIANGLE_TOPOLOGY.getNeighboursOf("d")).isEmpty();
    assertThat(TRIANGLE_TOPOLOGY.getNeighboursOf("x")).isEmpty();
    assertThat(TRIANGLE_TOPOLOGY.getNeighboursOf("y")).isEmpty();
  }

  @Test
  public void testGetNodeNeighbours() {
    assertThat(SINGLETON_TOPOLOGY.getNeighboursOf("a")).isEmpty();

    assertThat(TRIANGLE_TOPOLOGY.getNeighboursOf("a")).containsExactlyInAnyOrder("b");
    assertThat(TRIANGLE_TOPOLOGY.getNeighboursOf("b")).containsExactlyInAnyOrder("c");
    assertThat(TRIANGLE_TOPOLOGY.getNeighboursOf("c")).containsExactlyInAnyOrder("a");

    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.getNeighboursOf("a"))
        .containsExactlyInAnyOrder("b", "c");
    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.getNeighboursOf("b"))
        .containsExactlyInAnyOrder("a", "c");
    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.getNeighboursOf("c"))
        .containsExactlyInAnyOrder("a", "b");

    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.getNeighboursOf("a")).containsExactlyInAnyOrder("b");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.getNeighboursOf("b")).containsExactlyInAnyOrder("c");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.getNeighboursOf("c")).containsExactlyInAnyOrder("a");

    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.getNeighboursOf("x")).containsExactlyInAnyOrder("y");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.getNeighboursOf("y")).containsExactlyInAnyOrder("z");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.getNeighboursOf("z")).containsExactlyInAnyOrder("x");

    assertThat(STAR_TOPOLOGY.getNeighboursOf("source"))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(STAR_TOPOLOGY.getNeighboursOf("a")).isEmpty();
    assertThat(STAR_TOPOLOGY.getNeighboursOf("b")).isEmpty();
    assertThat(STAR_TOPOLOGY.getNeighboursOf("c")).isEmpty();
    assertThat(STAR_TOPOLOGY.getNeighboursOf("d")).isEmpty();
    assertThat(STAR_TOPOLOGY.getNeighboursOf("e")).isEmpty();

    assertThat(RING_TOPOLOGY.getNeighboursOf("a")).containsExactlyInAnyOrder("b");
    assertThat(RING_TOPOLOGY.getNeighboursOf("b")).containsExactlyInAnyOrder("c");
    assertThat(RING_TOPOLOGY.getNeighboursOf("c")).containsExactlyInAnyOrder("d");
    assertThat(RING_TOPOLOGY.getNeighboursOf("d")).containsExactlyInAnyOrder("e");
    assertThat(RING_TOPOLOGY.getNeighboursOf("e")).containsExactlyInAnyOrder("a");
  }

  @Test
  public void testRadialSearch() {
    assertThat(TRIANGLE_TOPOLOGY.performRadialSearch("a", 0)).containsExactlyInAnyOrder("a");
    assertThat(TRIANGLE_TOPOLOGY.performRadialSearch("a", 1)).containsExactlyInAnyOrder("a", "b");
    assertThat(TRIANGLE_TOPOLOGY.performRadialSearch("a", 2))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TRIANGLE_TOPOLOGY.performRadialSearch("a", 3))
        .containsExactlyInAnyOrder("a", "b", "c");

    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.performRadialSearch("a", 0))
        .containsExactlyInAnyOrder("a");
    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.performRadialSearch("a", 1))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.performRadialSearch("a", 2))
        .containsExactlyInAnyOrder("a", "b", "c");

    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("a", 0))
        .containsExactlyInAnyOrder("a");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("a", 1))
        .containsExactlyInAnyOrder("a", "b");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("a", 2))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("a", 3))
        .containsExactlyInAnyOrder("a", "b", "c");

    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("x", 0))
        .containsExactlyInAnyOrder("x");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("x", 1))
        .containsExactlyInAnyOrder("x", "y");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("x", 2))
        .containsExactlyInAnyOrder("x", "y", "z");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performRadialSearch("x", 3))
        .containsExactlyInAnyOrder("x", "y", "z");

    assertThat(STAR_TOPOLOGY.performRadialSearch("source", 0)).containsExactlyInAnyOrder("source");
    assertThat(STAR_TOPOLOGY.performRadialSearch("source", 1))
        .containsExactlyInAnyOrder("source", "a", "b", "c", "d", "e");
    assertThat(STAR_TOPOLOGY.performRadialSearch("source", 2))
        .containsExactlyInAnyOrder("source", "a", "b", "c", "d", "e");

    assertThat(RING_TOPOLOGY.performRadialSearch("a", 0)).containsExactlyInAnyOrder("a");
    assertThat(RING_TOPOLOGY.performRadialSearch("a", 1)).containsExactlyInAnyOrder("a", "b");
    assertThat(RING_TOPOLOGY.performRadialSearch("a", 2)).containsExactlyInAnyOrder("a", "b", "c");
    assertThat(RING_TOPOLOGY.performRadialSearch("a", 3))
        .containsExactlyInAnyOrder("a", "b", "c", "d");
    assertThat(RING_TOPOLOGY.performRadialSearch("a", 4))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(RING_TOPOLOGY.performRadialSearch("a", 5))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
  }

  @Test
  public void testFloodSearch() {
    assertThat(TRIANGLE_TOPOLOGY.performFloodSearch("a")).containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TRIANGLE_TOPOLOGY.performFloodSearch("b")).containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TRIANGLE_TOPOLOGY.performFloodSearch("c")).containsExactlyInAnyOrder("a", "b", "c");

    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.performFloodSearch("a"))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.performFloodSearch("b"))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(BI_DIRECTIONAL_TRIANGLE_TOPOLOGY.performFloodSearch("c"))
        .containsExactlyInAnyOrder("a", "b", "c");

    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performFloodSearch("a"))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performFloodSearch("b"))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performFloodSearch("c"))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performFloodSearch("x"))
        .containsExactlyInAnyOrder("x", "y", "z");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performFloodSearch("y"))
        .containsExactlyInAnyOrder("x", "y", "z");
    assertThat(TWO_ISLAND_TRIANGLE_TOPOLOGY.performFloodSearch("z"))
        .containsExactlyInAnyOrder("x", "y", "z");

    assertThat(STAR_TOPOLOGY.performFloodSearch("source"))
        .containsExactlyInAnyOrder("source", "a", "b", "c", "d", "e");
    assertThat(STAR_TOPOLOGY.performFloodSearch("a")).containsExactlyInAnyOrder("a");
    assertThat(STAR_TOPOLOGY.performFloodSearch("b")).containsExactlyInAnyOrder("b");
    assertThat(STAR_TOPOLOGY.performFloodSearch("c")).containsExactlyInAnyOrder("c");
    assertThat(STAR_TOPOLOGY.performFloodSearch("d")).containsExactlyInAnyOrder("d");
    assertThat(STAR_TOPOLOGY.performFloodSearch("e")).containsExactlyInAnyOrder("e");

    assertThat(RING_TOPOLOGY.performFloodSearch("a"))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(RING_TOPOLOGY.performFloodSearch("b"))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(RING_TOPOLOGY.performFloodSearch("c"))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(RING_TOPOLOGY.performFloodSearch("d"))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(RING_TOPOLOGY.performFloodSearch("e"))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
  }
}
