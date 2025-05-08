package fr.uge.net.adrien.client;

import java.nio.channels.Selector;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * A simple console thread. It reads user input and sends it to the BlockingQueue in a loop and wakes up the Selector.
 */
class Console implements Runnable {

  private final static Logger logger = Logger.getLogger(Console.class.getName());
  private final BlockingQueue<String> messages;
  private final Selector selector;

  /**
   * Creates a new Console thread.
   *
   * @param messages the BlockingQueue to send the user input to
   * @param selector the Selector to wake up when a message is put in the BlockingQueue
   */
  public Console(BlockingQueue<String> messages, Selector selector) {
    this.messages = Objects.requireNonNull(messages);
    this.selector = selector;
  }

  private void sendCommand(String msg) {
    Objects.requireNonNull(msg);
    messages.put(msg);
    selector.wakeup();
  }

  /**
   * Reads user input in a loop and sends it to the BlockingQueue.
   */
  @Override
  public void run() {
    logger.info("Started Console Thread");
    try (var scanner = new Scanner(System.in)) {
      while (scanner.hasNextLine()) {
        var str = scanner.nextLine();
        sendCommand(str);
      }
      logger.info("Console thread stopping");
    }
  }
}