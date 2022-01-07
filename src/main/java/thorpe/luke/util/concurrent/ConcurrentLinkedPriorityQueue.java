package thorpe.luke.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class ConcurrentLinkedPriorityQueue<E extends Comparable<E>> {
  private final AtomicInteger size;
  private final Node head;
  private final Node tail;

  public ConcurrentLinkedPriorityQueue() {
    this.size = new AtomicInteger(0);
    head = new Node(null);
    tail = new Node(null);
    head.setNext(tail);
  }

  private Node find(Predicate<E> condition) {
    head.lock();
    for (Node curr = head.next; curr != tail; curr = curr.next) {
      curr.lock();
      if (condition.test(curr.item)) {
        return curr;
      }
      curr.prev.unlock();
    }
    tail.lock();
    return tail;
  }

  private Node findSuccessorNodeFor(E target) {
    return find(item -> item.compareTo(target) >= 0);
  }

  public void offer(E item) {
    Node next = findSuccessorNodeFor(item);
    Node prev = next.prev;
    next.insertPrev(item);
    next.unlock();
    prev.unlock();
  }

  public E poll() {
    head.lock();
    head.next.lock();
    Node next = head.next;
    E item = head.pollNext();
    next.unlock();
    head.unlock();
    return item;
  }

  public int size() {
    return size.get();
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  private class Node {
    private final Lock lock = new ReentrantLock();
    private final E item;
    private Node prev;
    private Node next;

    private Node(E item) {
      this.item = item;
    }

    private void setPrev(Node prev) {
      prev.next = this;
      this.prev = prev;
    }

    private void setNext(Node next) {
      next.prev = this;
      this.next = next;
    }

    private void insertPrev(E item) {
      Node node = new Node(item);
      prev.setNext(node);
      this.setPrev(node);
      size.incrementAndGet();
    }

    private E pollNext() {
      E item = next.item;
      if (item == null) {
        return null;
      }
      this.setNext(next.next);
      size.decrementAndGet();
      return item;
    }

    private void lock() {
      lock.lock();
    }

    private void unlock() {
      lock.unlock();
    }
  }
}
