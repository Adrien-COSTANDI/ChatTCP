package fr.uge.net.adrien.client;

import fr.uge.net.adrien.Context;
import fr.uge.net.adrien.client.commands.Command;
import fr.uge.net.adrien.packets.ClientPublicMessage;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.packets.common.Address;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import static fr.uge.net.adrien.client.commands.Command.COMMAND_PREFIX;


public class Client {

  private static final Logger logger = Logger.getLogger(Client.class.getName());

  private final Thread console;
  private final BlockingQueue<String> consoleBlockingQueue;
  private final SocketChannel sc;
  private final InetSocketAddress serverAddress;
  private final Selector selector;
  private final Path pathToFolder;
  private WithServerContext withServerContext;

  private final String pseudo;
  private final String password;

  private final ServerSocketChannel serverSocketChannel;
  private final FriendManager friendManager;

  private final Address address;

  public Client(String pseudo, String password, InetSocketAddress serverAddress, Path pathToFolder)
      throws IOException {
    this.serverAddress = serverAddress;
    consoleBlockingQueue = new BlockingQueue<>();
    selector = Selector.open();
    console = Thread.ofPlatform().daemon().unstarted(new Console(consoleBlockingQueue, selector));
    sc = SocketChannel.open();
    this.pseudo = pseudo;
    this.password = password;
    this.friendManager = new FriendManager();
    this.pathToFolder = pathToFolder;

    var inetSocketAddress = new InetSocketAddress(0);
    serverSocketChannel = ServerSocketChannel.open();
    var tmp = serverSocketChannel.bind(inetSocketAddress);
    this.address = new Address((InetSocketAddress) tmp.getLocalAddress());
  }

  /**
   * Starts the console thread and the main loop to process commands sent from the console thread.
   */
  public void start() throws IOException {
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    sc.configureBlocking(false);
    var key = sc.register(selector, SelectionKey.OP_CONNECT);
    withServerContext = new WithServerContext(key, this);
    key.attach(withServerContext);
    sc.connect(serverAddress);

    console.start();

    logger.info("Client started");

    while (!Thread.interrupted()) {
      try {
        selector.select(this::treatKey);
        processInputs();
      } catch (InterruptedException e) {
        logger.warning("Console thread is dead");
      } catch (ClosedSelectorException e) {
        logger.warning("Selector is closed");
      } catch (UncheckedIOException e) {
        logger.warning(e.getCause().getMessage());
      }
    }
  }

  public void sendToServer(Packet packet) {
    withServerContext.send(packet);
  }

  public void sendToFriend(String pseudo, Packet packet) {
    friendManager.sendTo(pseudo, packet);
  }

  void addAlmostFriend(SocketAddress remoteAddress, FriendContext friendContext) {
    friendManager.addAlmostFriend(remoteAddress, friendContext);
  }

  void confirmFriendship(SocketAddress address, String pseudo) {
    friendManager.confirmFriendShip(address, pseudo);
  }

  public String getFriend(SocketAddress address) {
    return friendManager.getFriend(address);
  }

  public void makeANewFriend(String friend, SocketAddress address) {
    logger.info("Adding friend " + friend + " at " + address + " ...");
    try {
      var scFriend = SocketChannel.open();
      scFriend.configureBlocking(false);
      var key = scFriend.register(selector, SelectionKey.OP_CONNECT);
      var context = new FriendContext(key, this);
      key.attach(context);
      scFriend.connect(address);
      friendManager.addFriend(friend, context);
    } catch (IOException e) {
      logger.warning("Failed to connect to " + friend + ": " + e.getMessage());
      friendManager.removeFriend(friend);
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

  private void doAccept() throws IOException {
    var friend = serverSocketChannel.accept();
    if (friend == null) {
      return;
    }
    friend.configureBlocking(false);
    var friendKey = friend.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    var friendContext = new FriendContext(friendKey, this);
    friendKey.attach(friendContext);
    // add friend here, but I don't have the name
    addAlmostFriend(friend.getRemoteAddress(), friendContext);
  }

  private void treatKey(SelectionKey key) {
    try {
      if (key.isValid() && key.isAcceptable()) {
        doAccept(); // only serverSocketChannel
      }
      if (key.isValid() && key.isConnectable()) {
        ((ClientContext) key.attachment()).doConnect();
      }
      if (key.isValid() && key.isWritable()) {
        ((Context) key.attachment()).doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        ((Context) key.attachment()).doRead();
      }
    } catch (IOException ioe) {
      // lambda call in select requires to tunnel IOException
      throw new UncheckedIOException(ioe);
    }
  }

  public String pseudo() {
    return pseudo;
  }

  public Optional<String> password() {
    return Optional.ofNullable(password);
  }

  public Address address() {
    return address;
  }

  public void display(String str) {
    System.out.println(str);
  }

  /**
   * Processes the inputs from the BlockingQueue
   */
  private void processInputs() throws InterruptedException {
    while (!consoleBlockingQueue.isEmpty()) {
      var message = consoleBlockingQueue.take();
      if (message.isBlank()) {
        continue;
      }

      if (message.charAt(0) != COMMAND_PREFIX) {
        sendToServer(new ClientPublicMessage(message));
        continue;
      }

      try {
        Command.parse(message).execute(this);
      } catch (IllegalArgumentException e) {
        System.err.println(e.getMessage());
      }
    }
  }

  public static void main(String[] args) throws IOException {
    switch (args.length) {
      case 4 -> new Client(args[3],
                           null,
                           new InetSocketAddress(args[0], Integer.parseInt(args[1])),
                           Path.of(args[2])).start();
      case 5 -> new Client(args[3],
                           args[4],
                           new InetSocketAddress(args[0], Integer.parseInt(args[1])),
                           Path.of(args[2])).start();
      default -> System.out.println("Usage: java Client host port pathToFolder login [password]");
    }
  }
}
