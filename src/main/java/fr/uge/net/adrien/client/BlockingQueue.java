package fr.uge.net.adrien.client;

import java.util.ArrayDeque;
import java.util.Objects;

/**
 * A simple reimplementation of a blocking queue.
 *
 * @param <T>
 */
class BlockingQueue<T> {

  private final Object lock = new Object();
  private final ArrayDeque<T> queuedMessages = new ArrayDeque<>();

  /**
   * Puts a message in the queue and notifies the thread waiting for a message if the queue was empty.
   *
   * @param msg the message to put in the queue
   * @throws NullPointerException if msg is null
   */
  public void put(T msg) {
    Objects.requireNonNull(msg);
    synchronized (lock) {
      queuedMessages.add(msg);
      lock.notify();
    }
  }

  /**
   * Returns true if the queue is empty, false otherwise.
   *
   * @return true if the queue is empty, false otherwise.
   */
  public boolean isEmpty() {
    synchronized (lock) {
      return queuedMessages.isEmpty();
    }
  }

  /**
   * Takes a message from the queue and returns it.
   * This method blocks until a message is available if the queue was empty.
   *
   * @return the message taken from the queue
   * @throws InterruptedException if the thread is interrupted while waiting for a message
   */
  public T take() throws InterruptedException {
    synchronized (lock) {
      while (queuedMessages.isEmpty()) {
        lock.wait();
      }

      var message = queuedMessages.poll();
      if (message == null) {
        throw new AssertionError("Message is null");
      }
      return message;
    }
  }
}