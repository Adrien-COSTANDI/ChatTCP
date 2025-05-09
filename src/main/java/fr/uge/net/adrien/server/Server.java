package fr.uge.net.adrien.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class Server {

  public static final int BUFFER_SIZE = 1024;
  private static final Logger logger = Logger.getLogger(Server.class.getName());
  private static final int PORT = 9999;

  private final ServerSocketChannel serverSocketChannel;

  private final static int NB_THREADS_SERVER = 3;
  private final ArrayList<Thread> threads = new ArrayList<>(NB_THREADS_SERVER);
  private final ArrayList<NetworkThread> networkThreads = new ArrayList<>(NB_THREADS_SERVER);
  private int roundRobinIndex = 0;

  private final Set<String> connectedUsers = Collections.synchronizedSet(new HashSet<>());

  public Server() throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(PORT));

    for (int i = 0; i < NB_THREADS_SERVER; i++) {
      var networkThread = new NetworkThread(this);
      threads.add(Thread.ofPlatform().name("network-thread-" + i).unstarted(networkThread));
      networkThreads.add(networkThread);
    }
  }

  /// Starts the server by initializing and managing client connections through multiple threads.
  ///
  /// This method performs the following operations:
  /// - It starts all previously created network threads.
  /// - Enters a blocking loop to accept incoming client connections.
  /// - When a client connection is accepted:
  ///   - Delegates the accepted connection to one of the network threads.
  /// - Handles `IOException` during the connection acceptance, logging the error and shutting down the server.
  ///
  /// The method continues running until the current thread is interrupted.
  public void start() {
    threads.forEach(Thread::start);

    logger.info("Server started on port " + PORT);

    while (!Thread.interrupted()) {
      try {
        var client = serverSocketChannel.accept();
        logger.info("Accepted connection from " + client.getRemoteAddress());
        giveToAThread(client);
      } catch (IOException e) {
        logger.severe("Server Socket failed to accept connection: " + e.getMessage());
        shutdown();
        return;
      }
    }
  }

  private void shutdown() {
    logger.warning("Shutting down server");
    networkThreads.forEach(NetworkThread::shutdown);
    threads.forEach(Thread::interrupt);
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      // nothing
    }
  }

  private void giveToAThread(SocketChannel client) {
    networkThreads.get(roundRobinIndex).takeCareOf(client);
    roundRobinIndex = (roundRobinIndex + 1) % NB_THREADS_SERVER;
  }

  /**
   * Adds a user to the collection of connected users and checks if the user is unique.
   * If the user is successfully added to the set of connected users (i.e., the user
   * was not already present), the method returns true. Otherwise, it returns false.
   *
   * @param user the username of the user to add to the connected users set
   * @return true if the user was successfully added (i.e., the user is unique),
   * false if the user already exists in the set
   */
  public boolean addConnectedUserAndCheckUnique(String user) {
    logger.info("User " + user + " connected");
    return connectedUsers.add(user);
  }

  /**
   * Removes a user from the collection of currently connected users.
   *
   * @param user the username of the user to be removed from the connected users set
   */
  public void removeConnectedUser(String user) {
    logger.info("User " + user + " disconnected");
    connectedUsers.remove(user);
  }

  public static void main(String[] args) {
    try {
      new Server().start();
    } catch (IOException e) {
      logger.severe("Server failed to start: " + e.getMessage());
      System.exit(1);
    }
  }
}
