package fr.uge.net.adrien.server;

import fr.uge.net.adrien.packets.Packet;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

class NetworkThread implements Runnable {

  private static final Logger logger = Logger.getLogger(NetworkThread.class.getName());
  private final Selector localSelector;
  private String threadName;
  private final Server server;

  public NetworkThread(Server server) throws IOException {
    localSelector = Selector.open();
    this.server = server;
  }

  public void takeCareOf(SocketChannel client) {
    try {
      client.configureBlocking(false);
      var selectionKey = client.register(localSelector, SelectionKey.OP_READ);
      selectionKey.attach(new ServerContext(selectionKey, server));
      localSelector.wakeup();
    } catch (IOException e) {
      logger.warning(e.getMessage());
      silentlyClose(client);
    }
  }

  private void treatKey(SelectionKey key) {
    var serverContext = (ServerContext) key.attachment();
    try {
      if (key.isValid() && key.isWritable()) {
        serverContext.doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        serverContext.doRead();
      }
    } catch (IOException e) {
      server.removeConnectedUser(serverContext.clientPseudo());
      silentlyClose(key.channel());
    }
  }

  private void silentlyClose(Channel sc) {
    try {
      sc.close();
    } catch (IOException e) {
      // ignore exception
    }
  }

  public void shutdown() {
    try {
      localSelector.close();
    } catch (IOException e) {
      // nothing
    }
  }

  @Override
  public void run() {
    threadName = Thread.currentThread().getName();

    while (!Thread.interrupted()) {
      try {
        localSelector.select(this::treatKey);
      } catch (IOException e) {
        Thread.currentThread().interrupt();
        System.out.println(threadName + " : " + e.getMessage());
      } catch (ClosedSelectorException e) {
        logger.warning("Selector is closed");
      }
    }
    logger.warning(threadName + " is down");
  }

  public void localBroadcast(Packet packet) {
    localSelector.keys().forEach(key -> {
      var context = (ServerContext) key.attachment();
      context.send(packet);
      localSelector.wakeup();
    });
  }
}
