package fr.uge.net.adrien.client;

import static fr.uge.net.adrien.client.commands.Command.COMMAND_PREFIX;

import fr.uge.net.adrien.client.commands.Command;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;


public class Client {

  private static final Logger logger = Logger.getLogger(Client.class.getName());

  private final Thread console;
  private final BlockingQueue<String> consoleBlockingQueue;
  private final SocketChannel sc;
  private final InetSocketAddress serverAddress;
  private final Selector selector;

  public Client(InetSocketAddress serverAddress) throws IOException {
    this.serverAddress = serverAddress;
    consoleBlockingQueue = new BlockingQueue<>();
    selector = Selector.open();
    console = new Thread(new Console(consoleBlockingQueue, selector));
    sc = SocketChannel.open();
  }

  /**
   * Starts the console thread and the main loop to process commands sent from the console thread.
   */
  public void start() throws IOException {
    sc.configureBlocking(false);
    sc.register(selector, SelectionKey.OP_CONNECT);

    sc.connect(serverAddress);

    console.start();

    while (!Thread.interrupted()) {
      try {
        selector.select(this::treatKey);
        processCommands();
      } catch (InterruptedException e) {
        logger.warning("Console thread is dead");
      }
    }
  }

  private void treatKey(SelectionKey key) {
    try {
      if (key.isValid() && key.isConnectable()) {
        if (!sc.finishConnect()) {
          return; // the selector gave a bad hint
        }
        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        System.out.println("my adress : " + sc.getLocalAddress());
      }
    } catch (IOException ioe) {
      // lambda call in select requires to tunnel IOException
      throw new UncheckedIOException(ioe);
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


  public static void main(String[] args) throws IOException {
    new Client(new InetSocketAddress("localhost", 9999)).start();
  }
}
