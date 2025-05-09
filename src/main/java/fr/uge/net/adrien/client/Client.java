package fr.uge.net.adrien.client;

import fr.uge.net.adrien.client.commands.Command;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import static fr.uge.net.adrien.client.commands.Command.COMMAND_PREFIX;


public class Client {

  private static final Logger logger = Logger.getLogger(Client.class.getName());

  private final Thread console;
  private final BlockingQueue<String> consoleBlockingQueue;
  private final SocketChannel sc;
  private final InetSocketAddress serverAddress;
  private final Selector selector;
  private Context uniqueContext;

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
    var key = sc.register(selector, SelectionKey.OP_CONNECT);
    uniqueContext = new Context(key, this);
    sc.connect(serverAddress);

    console.start();

    logger.info("Client started");

    while (!Thread.interrupted()) {
      try {
        selector.select(this::treatKey);
        processCommands();
      } catch (InterruptedException e) {
        logger.warning("Console thread is dead");
      } catch (ClosedSelectorException e) {
        logger.warning("Selector is closed");
      }
    }
  }

  public void shutdown() {
    logger.info("Shutting down client");
    console.interrupt();
    try {
      sc.close();
      selector.close();
    } catch (IOException e) {
      // ignore exception
    }
    Thread.currentThread().interrupt();
  }

  private void treatKey(SelectionKey key) {
    try {
      if (key.isValid() && key.isConnectable()) {
        uniqueContext.doConnect();
      }
      if (key.isValid() && key.isWritable()) {
        uniqueContext.doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        uniqueContext.doRead();
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
