package thorpe.luke.network.simulation.node;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.stream.Collectors;
import org.junit.Test;

public class NodeTopologyTest {
  public static final NodeTopology EMPTY_NODE_TOPOLOGY = NodeTopology.builder().build();

  public static final NodeTopology SINGLETON_NODE_TOPOLOGY =
      NodeTopology.builder().addNode("a").build();

  public static final NodeTopology TRIANGLE_NODE_TOPOLOGY =
      NodeTopology.builder()
          .addNode("a")
          .addNode("b")
          .addNode("c")
          .addConnection("a", "b")
          .addConnection("b", "c")
          .addConnection("c", "a")
          .build();

  public static final NodeTopology BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY =
      NodeTopology.builder()
          .addNode("a")
          .addNode("b")
          .addNode("c")
          .addConnection("a", "b")
          .addConnection("a", "c")
          .addConnection("b", "a")
          .addConnection("b", "c")
          .addConnection("c", "a")
          .addConnection("c", "b")
          .build();

  public static final NodeTopology TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY =
      NodeTopology.builder()
          .addNode("a")
          .addNode("b")
          .addNode("c")
          .addConnection("a", "b")
          .addConnection("b", "c")
          .addConnection("c", "a")
          .addNode("x")
          .addNode("y")
          .addNode("z")
          .addConnection("x", "y")
          .addConnection("y", "z")
          .addConnection("z", "x")
          .build();

  public static final NodeTopology STAR_NODE_TOPOLOGY =
      NodeTopology.builder()
          .addNode("x")
          .addNode("a")
          .addNode("b")
          .addNode("c")
          .addNode("d")
          .addNode("e")
          .addConnection("x", "a")
          .addConnection("x", "b")
          .addConnection("x", "c")
          .addConnection("x", "d")
          .addConnection("x", "e")
          .build();

  public static final NodeTopology RING_NODE_TOPOLOGY =
      NodeTopology.builder()
          .addNode("a")
          .addNode("b")
          .addNode("c")
          .addNode("d")
          .addNode("e")
          .addConnection("a", "b")
          .addConnection("b", "c")
          .addConnection("c", "d")
          .addConnection("d", "e")
          .addConnection("e", "a")
          .build();

  public Collection<String> namesOf(Collection<NodeAddress> nodeAddresses) {
    return nodeAddresses.stream().map(NodeAddress::getName).collect(Collectors.toList());
  }

  @Test
  public void testGetNodes() {
    assertThat(EMPTY_NODE_TOPOLOGY.getNodeAddresses()).isEmpty();
    assertThat(namesOf(SINGLETON_NODE_TOPOLOGY.getNodeAddresses())).containsExactlyInAnyOrder("a");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.getNodeAddresses()))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.getNodeAddresses()))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.getNodeAddresses()))
        .containsExactlyInAnyOrder("a", "b", "c", "x", "y", "z");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.getNodeAddresses()))
        .containsExactlyInAnyOrder("x", "a", "b", "c", "d", "e");
    assertThat(namesOf(RING_NODE_TOPOLOGY.getNodeAddresses()))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
  }

  @Test
  public void testUndefinedNodeHasNoNeighbours() {
    assertThat(EMPTY_NODE_TOPOLOGY.getNeighboursOf("a")).isEmpty();
    assertThat(EMPTY_NODE_TOPOLOGY.getNeighboursOf("b")).isEmpty();
    assertThat(EMPTY_NODE_TOPOLOGY.getNeighboursOf("x")).isEmpty();
    assertThat(EMPTY_NODE_TOPOLOGY.getNeighboursOf("y")).isEmpty();

    assertThat(SINGLETON_NODE_TOPOLOGY.getNeighboursOf("b")).isEmpty();
    assertThat(SINGLETON_NODE_TOPOLOGY.getNeighboursOf("y")).isEmpty();

    assertThat(TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("d")).isEmpty();
    assertThat(TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("x")).isEmpty();
    assertThat(TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("y")).isEmpty();
  }

  @Test
  public void testGetNodeNeighbours() {
    assertThat(SINGLETON_NODE_TOPOLOGY.getNeighboursOf("a")).isEmpty();

    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("a"))).containsExactlyInAnyOrder("b");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("b"))).containsExactlyInAnyOrder("c");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("c"))).containsExactlyInAnyOrder("a");

    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("a")))
        .containsExactlyInAnyOrder("b", "c");
    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("b")))
        .containsExactlyInAnyOrder("a", "c");
    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("c")))
        .containsExactlyInAnyOrder("a", "b");

    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("a")))
        .containsExactlyInAnyOrder("b");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("b")))
        .containsExactlyInAnyOrder("c");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("c")))
        .containsExactlyInAnyOrder("a");

    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("x")))
        .containsExactlyInAnyOrder("y");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("y")))
        .containsExactlyInAnyOrder("z");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.getNeighboursOf("z")))
        .containsExactlyInAnyOrder("x");

    assertThat(namesOf(STAR_NODE_TOPOLOGY.getNeighboursOf("x")))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(STAR_NODE_TOPOLOGY.getNeighboursOf("a")).isEmpty();
    assertThat(STAR_NODE_TOPOLOGY.getNeighboursOf("b")).isEmpty();
    assertThat(STAR_NODE_TOPOLOGY.getNeighboursOf("c")).isEmpty();
    assertThat(STAR_NODE_TOPOLOGY.getNeighboursOf("d")).isEmpty();
    assertThat(STAR_NODE_TOPOLOGY.getNeighboursOf("e")).isEmpty();

    assertThat(namesOf(RING_NODE_TOPOLOGY.getNeighboursOf("a"))).containsExactlyInAnyOrder("b");
    assertThat(namesOf(RING_NODE_TOPOLOGY.getNeighboursOf("b"))).containsExactlyInAnyOrder("c");
    assertThat(namesOf(RING_NODE_TOPOLOGY.getNeighboursOf("c"))).containsExactlyInAnyOrder("d");
    assertThat(namesOf(RING_NODE_TOPOLOGY.getNeighboursOf("d"))).containsExactlyInAnyOrder("e");
    assertThat(namesOf(RING_NODE_TOPOLOGY.getNeighboursOf("e"))).containsExactlyInAnyOrder("a");
  }

  @Test
  public void testBreadthFirstQuery() {
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 0)))
        .containsExactly("a");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 1)))
        .containsExactly("a", "b");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 2)))
        .containsExactly("a", "b", "c");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 3)))
        .containsExactly("a", "b", "c");

    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 0)))
        .containsExactlyInAnyOrder("a");
    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 1)))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 2)))
        .containsExactlyInAnyOrder("a", "b", "c");

    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 0)))
        .containsExactly("a");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 1)))
        .containsExactly("a", "b");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 2)))
        .containsExactly("a", "b", "c");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 3)))
        .containsExactly("a", "b", "c");

    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("x", 0)))
        .containsExactly("x");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("x", 1)))
        .containsExactly("x", "y");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("x", 2)))
        .containsExactly("x", "y", "z");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByBreadthFirstSearch("x", 3)))
        .containsExactly("x", "y", "z");

    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByBreadthFirstSearch("x", 0)))
        .containsExactlyInAnyOrder("x");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByBreadthFirstSearch("x", 1)))
        .containsExactlyInAnyOrder("x", "a", "b", "c", "d", "e");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByBreadthFirstSearch("x", 2)))
        .containsExactlyInAnyOrder("x", "a", "b", "c", "d", "e");

    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 0))).containsExactly("a");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 1)))
        .containsExactly("a", "b");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 2)))
        .containsExactly("a", "b", "c");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 3)))
        .containsExactly("a", "b", "c", "d");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 4)))
        .containsExactly("a", "b", "c", "d", "e");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByBreadthFirstSearch("a", 5)))
        .containsExactly("a", "b", "c", "d", "e");
  }

  @Test
  public void testFloodQuery() {
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("a")))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("b")))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("c")))
        .containsExactlyInAnyOrder("a", "b", "c");

    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("a")))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("b")))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(BI_DIRECTIONAL_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("c")))
        .containsExactlyInAnyOrder("a", "b", "c");

    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("a")))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("b")))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("c")))
        .containsExactlyInAnyOrder("a", "b", "c");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("x")))
        .containsExactlyInAnyOrder("x", "y", "z");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("y")))
        .containsExactlyInAnyOrder("x", "y", "z");
    assertThat(namesOf(TWO_ISLAND_TRIANGLE_NODE_TOPOLOGY.queryByFloodingFrom("z")))
        .containsExactlyInAnyOrder("x", "y", "z");

    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByFloodingFrom("x")))
        .containsExactlyInAnyOrder("x", "a", "b", "c", "d", "e");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByFloodingFrom("a"))).containsExactlyInAnyOrder("a");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByFloodingFrom("b"))).containsExactlyInAnyOrder("b");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByFloodingFrom("c"))).containsExactlyInAnyOrder("c");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByFloodingFrom("d"))).containsExactlyInAnyOrder("d");
    assertThat(namesOf(STAR_NODE_TOPOLOGY.queryByFloodingFrom("e"))).containsExactlyInAnyOrder("e");

    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByFloodingFrom("a")))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByFloodingFrom("b")))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByFloodingFrom("c")))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByFloodingFrom("d")))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    assertThat(namesOf(RING_NODE_TOPOLOGY.queryByFloodingFrom("e")))
        .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
  }
}
