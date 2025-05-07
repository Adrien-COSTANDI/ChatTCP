package fr.uge.net.adrien.client;

import static fr.uge.net.adrien.client.commands.Command.COMMAND_PREFIX;

import fr.uge.net.adrien.client.commands.Command;
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
   * Processes the message from the BlockingQueue
   */
  private void processCommands() throws InterruptedException {
    while (!consoleBlockingQueue.isEmpty()) {
      var message = consoleBlockingQueue.take();

      if (message.charAt(0) == COMMAND_PREFIX) {
        try {
          Command.parse(message).execute();
        } catch (IllegalArgumentException e) {
          System.err.println(e.getMessage());
        }
      } else {
        System.out.println("received: " + message);
      }
    }
  }


  public static void main(String[] args) {
    new Client().start();
  }
}
