package fr.uge.net.adrien.client;

import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * A simple console thread. It reads user input and sends it to the BlockingQueue in a loop.
 */
class Console implements Runnable {

    private final static Logger logger = Logger.getLogger(Console.class.getName());
    private final BlockingQueue<String> messages;

  /**
   * Creates a new Console thread.
   * @param messages the BlockingQueue to send the user input to
   */
  public Console(BlockingQueue<String> messages) {
      this.messages = Objects.requireNonNull(messages);
    }

    private void sendCommand(String msg) {
      Objects.requireNonNull(msg);
      messages.put(msg);
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