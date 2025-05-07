package fr.uge.net.adrien.client;

import java.util.logging.Logger;


public class Client {

  private static final Logger logger = Logger.getLogger(Client.class.getName());

  private final Thread console;
  private final BlockingQueue<String> consoleBlockingQueue;

  public Client() {
    consoleBlockingQueue = new BlockingQueue<>();
    console = new Thread(new Console(consoleBlockingQueue));
  }

  /**
   * Starts the console thread and the main loop to process commands sent from the console thread.
   */
  public void start() {
    console.start();

    while (!Thread.interrupted()) {
      try {
        processCommands();
      } catch (InterruptedException e) {
        logger.warning("Console thread is dead");
      }
    }
  }

  /**
   * Processes the command from the BlockingQueue
   */
  private void processCommands() throws InterruptedException {
    while (!consoleBlockingQueue.isEmpty()) {
      var command = consoleBlockingQueue.take();
      System.out.println("received: " + command);
    }
  }


  public static void main(String[] args) {
    new Client().start();
  }
}
