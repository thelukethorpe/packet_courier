package thorpe.luke.util.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

public class ConcurrentLinkedPriorityQueueTest {

  @Test
  public void testOrderingIsPreservedDuringChaoticOffering() {
    int numberOfOfferingThreads = 25;
    int numberOfOperations = 1_000;

    // Get an extra partition for negative testing.
    List<Integer>[] offeredValues =
        uniquePartitionedNumbers(numberOfOfferingThreads + 1, numberOfOperations);
    List<Integer> unusedOfferedValues = offeredValues[numberOfOfferingThreads];

    ConcurrentLinkedPriorityQueue<Integer> concurrentLinkedPriorityQueue =
        new ConcurrentLinkedPriorityQueue<>();
    // No need to worry about concurrent reads / writes here,
    // since data is partitioned per thread and as such is essentially serial.
    Collection<Thread> offeringThreads =
        IntStream.range(0, numberOfOfferingThreads)
            .mapToObj(
                i ->
                    new Thread(
                        () -> offeredValues[i].forEach(concurrentLinkedPriorityQueue::offer)))
            .collect(Collectors.toList());

    // Set threads to work.
    offeringThreads.forEach(Thread::start);
    offeringThreads.forEach(
        thread -> {
          try {
            thread.join();
          } catch (InterruptedException e) {
            fail(e.getMessage());
          }
        });

    // Concatenate results.
    List<Integer> allOfferedValues =
        Arrays.stream(offeredValues).flatMap(List::stream).collect(Collectors.toList());
    allOfferedValues.removeAll(unusedOfferedValues);
    List<Integer> allPolledValues = new ArrayList<>(concurrentLinkedPriorityQueue.size());
    while (!concurrentLinkedPriorityQueue.isEmpty()) {
      Integer polledValue = concurrentLinkedPriorityQueue.poll();
      assertThat(polledValue).isNotNull();
      allPolledValues.add(polledValue);
    }
    assertThat(concurrentLinkedPriorityQueue.poll()).isNull();

    // Check for positive integrity.
    assertThat(allOfferedValues).hasSameSizeAs(allPolledValues);
    assertThat(allOfferedValues).containsExactlyInAnyOrderElementsOf(allPolledValues);
    assertThat(allPolledValues).isSorted();

    // Check for negative integrity.
    assertThat(allPolledValues).doesNotContainAnyElementsOf(unusedOfferedValues);
  }

  @Test
  public void testMonkeyRobustness() {
    int numberOfOfferingThreads = 25;
    int numberOfPollingThreads = 5;
    int numberOfOperations = 1_000;

    // Get an extra partition for negative testing.
    List<Integer>[] offeredValues =
        uniquePartitionedNumbers(numberOfOfferingThreads + 1, numberOfOperations);
    List<Integer> unusedOfferedValues = offeredValues[numberOfOfferingThreads];
    List<Integer>[] polledValues = new List[numberOfPollingThreads];

    ConcurrentLinkedPriorityQueue<Integer> concurrentLinkedPriorityQueue =
        new ConcurrentLinkedPriorityQueue<>();
    // No need to worry about concurrent reads / writes here,
    // since data is partitioned per thread and as such is essentially serial.
    AtomicInteger numberOfOfferingThreadsComplete = new AtomicInteger(0);
    Collection<Thread> offeringThreads =
        IntStream.range(0, numberOfOfferingThreads)
            .mapToObj(
                i ->
                    new Thread(
                        () -> {
                          offeredValues[i].forEach(concurrentLinkedPriorityQueue::offer);
                          numberOfOfferingThreadsComplete.incrementAndGet();
                        }))
            .collect(Collectors.toList());
    Collection<Thread> pollingThreads =
        IntStream.range(0, numberOfPollingThreads)
            .mapToObj(
                i -> {
                  polledValues[i] = new LinkedList<>();
                  return new Thread(
                      () -> {
                        while (numberOfOfferingThreadsComplete.get() < numberOfOfferingThreads
                            || !concurrentLinkedPriorityQueue.isEmpty()) {
                          Integer polledValue = concurrentLinkedPriorityQueue.poll();
                          if (polledValue != null) {
                            polledValues[i].add(polledValue);
                          }
                        }
                      });
                })
            .collect(Collectors.toList());

    // Set threads to work.
    offeringThreads.forEach(Thread::start);
    pollingThreads.forEach(Thread::start);
    offeringThreads.forEach(
        thread -> {
          try {
            thread.join();
          } catch (InterruptedException e) {
            fail(e.getMessage());
          }
        });
    pollingThreads.forEach(
        thread -> {
          try {
            thread.join();
          } catch (InterruptedException e) {
            fail(e.getMessage());
          }
        });

    // Concatenate results.
    List<Integer> allOfferedValues =
        Arrays.stream(offeredValues).flatMap(List::stream).collect(Collectors.toList());
    allOfferedValues.removeAll(unusedOfferedValues);
    List<Integer> allPolledValues =
        Arrays.stream(polledValues).flatMap(List::stream).collect(Collectors.toList());

    // Check for positive integrity.
    assertThat(concurrentLinkedPriorityQueue.isEmpty()).isTrue();
    assertThat(allOfferedValues).hasSameSizeAs(allPolledValues);
    assertThat(allOfferedValues).containsExactlyInAnyOrderElementsOf(allPolledValues);

    // Check for negative integrity.
    assertThat(allPolledValues).doesNotContainAnyElementsOf(unusedOfferedValues);
  }

  private List<Integer>[] uniquePartitionedNumbers(int width, int depth) {
    List<Integer> numbers =
        IntStream.range(0, width * depth).boxed().collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(numbers);
    List<Integer>[] partition = new List[width];
    for (int i = 0; i < width; i++) {
      partition[i] = numbers.subList(i * depth, (i + 1) * depth);
    }
    return partition;
  }
}
