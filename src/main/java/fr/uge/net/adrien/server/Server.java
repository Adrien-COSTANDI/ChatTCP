package fr.uge.net.adrien.server;

import fr.uge.net.adrien.packets.Packet;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

  private final Map<String, ServerContext> connectedUsersContexts = new ConcurrentHashMap<>();
  private final Database database = Database.getInstance();

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
   * Attempts to connect a user to the server using the provided pseudo and optional password.
   * <p>
   * If a password is not provided, the method checks if the pseudo already exists in the database.
   * If the pseudo does not exist, the user is added to the connected users. If a password is provided,
   * the method verifies the pseudo-password combination before attempting to add the user to the connected users.
   *
   * @param pseudo the username to connect
   * @param password an optional password associated with the username
   * @return the connection status:
   *         - {@code ConnectStatus.OK} if the connection is successful
   *         - {@code ConnectStatus.USERNAME_EXISTS} if the user is already connected
   *         - {@code ConnectStatus.INVALID_USER_OR_PASSWORD} if the pseudo or password is invalid
   */
  ConnectStatus connect(String pseudo, Optional<String> password, ServerContext context) {
    if (password.isPresent()) {
      if (!database.passwordMatch(pseudo, password.get())) {
        return ConnectStatus.INVALID_USER_OR_PASSWORD;
      }
      return putAndCheckIfUsernameAvailable(pseudo, context);
    }

    if (database.usernameExists(pseudo)) {
      return ConnectStatus.USERNAME_EXISTS;
    }
    return putAndCheckIfUsernameAvailable(pseudo, context);

  }

  private ConnectStatus putAndCheckIfUsernameAvailable(String pseudo, ServerContext context) {
    var previousContext = connectedUsersContexts.putIfAbsent(pseudo, context);
    if (previousContext != null) {
      return ConnectStatus.USERNAME_EXISTS;
    }
    return ConnectStatus.OK;
  }

  /**
   * Removes a user from the collection of currently connected users.
   *
   * @param user the username of the user to be removed from the connected users set
   */
  public void removeConnectedUser(String user) {
    logger.info("User " + user + " disconnected");
    connectedUsersContexts.remove(user);
  }

  public static void main(String[] args) {
    try {
      new Server().start();
    } catch (IOException e) {
      logger.severe("Server failed to start: " + e.getMessage());
      System.exit(1);
    }
  }

  public void broadcast(Packet packet) {
    for (var networkThread : networkThreads) {
      networkThread.localBroadcast(packet);
    }
  }

  public void sendTo(String pseudo, Packet packet) {
    var context = connectedUsersContexts.get(pseudo);
    if (context == null) {
      throw new IllegalStateException(
          "Client not found: " + pseudo + " (expected one of: " + connectedUsersContexts.keySet() +
          ")");
    }
    context.send(packet);
  }
}
