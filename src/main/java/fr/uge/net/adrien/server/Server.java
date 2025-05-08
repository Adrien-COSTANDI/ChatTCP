package fr.uge.net.adrien.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Logger;

public class Server {

  public static final int BUFFER_SIZE = 1024;
  private static final Logger logger = Logger.getLogger(Server.class.getName());
  private static final int PORT = 9999;

  private final ServerSocketChannel serverSocketChannel;

  public Server() throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(PORT));
  }

  public void start() {
    while (!Thread.interrupted()) {
      try {
        var client = serverSocketChannel.accept();
        logger.info("Accepted connection from " + client.getRemoteAddress());
      } catch (IOException e) {
        logger.severe("Server Socket failed to accept connection: " + e.getMessage());
        System.exit(1);
        return;
      }
    }
  }
}
