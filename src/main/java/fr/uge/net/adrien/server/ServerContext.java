package fr.uge.net.adrien.server;

import fr.uge.net.adrien.Context;
import fr.uge.net.adrien.packets.ClientPublicMessage;
import fr.uge.net.adrien.packets.ConnectAuth;
import fr.uge.net.adrien.packets.ConnectNoAuth;
import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.packets.DmConnect;
import fr.uge.net.adrien.packets.DmRequest;
import fr.uge.net.adrien.packets.DmResponse;
import fr.uge.net.adrien.packets.DmText;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.packets.ServerForwardPublicMessage;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.packets.PacketReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

class ServerContext implements Context {

  private final ByteBuffer bufferIn = ByteBuffer.allocate(Server.BUFFER_SIZE);
  private final ByteBuffer bufferOut = ByteBuffer.allocate(Server.BUFFER_SIZE);
  private final PacketReader packetReader = new PacketReader();
  private final ArrayDeque<Packet> queue = new ArrayDeque<>();

  private boolean closed = false;

  private final SelectionKey key;
  private final SocketChannel sc;
  private final Server server;

  private String clientPseudo;

  public ServerContext(SelectionKey key, Server server) {
    this.key = Objects.requireNonNull(key);
    this.sc = (SocketChannel) key.channel();
    this.server = Objects.requireNonNull(server);
  }

  public String clientPseudo() {
    return clientPseudo;
  }

  private void processReceivedPacket(Packet packet) {
    System.out.println("received " + packet);

    switch (packet) {
      case ConnectServerResponse _, ServerForwardPublicMessage _, DmText _, DmConnect _ -> {
      }
      case ConnectNoAuth connectNoAuth -> {
        switch (server.connect(connectNoAuth.pseudo(), Optional.empty(), this)) {
          case INVALID_USER_OR_PASSWORD ->
              throw new AssertionError("Should not happen in NoAuth mode");
          case USERNAME_EXISTS -> {
            System.out.println(
                "Failed to connect user: " + connectNoAuth.pseudo() + " already exists");
            send(new ConnectServerResponse(ConnectServerResponse.StatusCode.PSEUDO_ALREADY_TAKEN));
          }
          case OK -> {
            clientPseudo = connectNoAuth.pseudo();
            send(new ConnectServerResponse(ConnectServerResponse.StatusCode.OK));
          }
        }
      }
      case ConnectAuth connectAuth -> {
        switch (server.connect(connectAuth.pseudo(), Optional.of(connectAuth.password()), this)) {
          case INVALID_USER_OR_PASSWORD -> {
            System.out.println(
                "Failed to connect user: " + connectAuth.pseudo() + " invalid pseudo or password");
            send(new ConnectServerResponse(ConnectServerResponse.StatusCode.INVALID_PSEUDO_OR_PASSWORD));
          }
          case USERNAME_EXISTS -> {
            System.out.println(
                "Failed to connect user: " + connectAuth.pseudo() + " already exists");
            send(new ConnectServerResponse(ConnectServerResponse.StatusCode.PSEUDO_ALREADY_TAKEN));
          }
          case OK -> {
            clientPseudo = connectAuth.pseudo();
            send(new ConnectServerResponse(ConnectServerResponse.StatusCode.OK));
          }
        }
      }
      case ClientPublicMessage clientPublicMessage -> {
        System.out.println(
            "broadcasting " + clientPublicMessage.contenu() + " from " + clientPseudo +
                           " to all users");
        server.broadcast(new ServerForwardPublicMessage(clientPublicMessage.contenu(),
                                                        clientPseudo));
      }
      case DmRequest dmRequest -> {
        server.sendTo(dmRequest.pseudo(), new DmRequest(clientPseudo));
      }
      case DmResponse dmResponse -> {
        server.sendTo(dmResponse.pseudo(),
                      new DmResponse(clientPseudo,
                                     dmResponse.ok(),
                                     dmResponse.nonce(),
                                     dmResponse.address()));
      }
    }
  }

  /**
   * Sends a packet by adding it to the queue for processing, updating the buffer
   * for outbound data, and modifying the interest operations of the associated key.
   *
   * @param packet the packet to be sent, which will be added to the processing queue
   */
  public void send(Packet packet) {
    System.out.println("sending " + packet);
    queue.add(packet);
    processOut();
    updateInterestOps();
  }

  /**
   * Process the content of bufferIn
   * <p>
   * The convention is that bufferIn is in write-mode before the call to process
   * and after the call
   */
  private void processIn() {
    Reader.ProcessStatus status;

    while ((status = packetReader.process(bufferIn)) == Reader.ProcessStatus.DONE) {
      var packet = packetReader.get();
      packetReader.reset();
      processReceivedPacket(packet);
    }
    if (status == Reader.ProcessStatus.ERROR) {
      System.out.println("bad packet read, bye bye");
      silentlyClose();
    }
  }

  private void processOut() {
    while (!queue.isEmpty() && bufferOut.remaining() >= queue.peek().length()) {
      var packetBuffer = queue.poll().toByteBuffer().flip();
      bufferOut.put(packetBuffer);
    }
  }

  /**
   * Update the interestOps of the key looking only at values of the boolean
   * closed and of both ByteBuffers.
   * <p>
   * The convention is that both buffers are in write-mode before the call to
   * updateInterestOps and after the call. Also, it is assumed that a "process" method has
   * been called just before updateInterestOps.
   */
  private void updateInterestOps() {
    var ops = 0;

    if (!closed && bufferIn.hasRemaining()) {
      ops |= OP_READ;
    }

    if (bufferOut.position() != 0) {
      ops |= OP_WRITE;
    }

    if (ops == 0) {
      silentlyClose();
      return;
    }

    key.interestOps(ops);
    key.selector().wakeup();
  }

  private void silentlyClose() {
    try {
      sc.close();
    } catch (IOException e) {
      // ignore exception
    }
  }

  /**
   * Performs the read action on sc
   * <p>
   * The convention is that both buffers are in write-mode before the call to
   * doRead and after the call
   *
   * @throws IOException if an IOException occurs
   */
  public void doRead() throws IOException {
    if (sc.read(bufferIn) == -1) {
      closed = true;
    }
    processIn();
    updateInterestOps();
  }

  /**
   * Performs the write action on sc
   * <p>
   * The convention is that both buffers are in write-mode before the call to
   * doWrite and after the call
   *
   * @throws IOException if an IOException occurs
   */
  public void doWrite() throws IOException {
    bufferOut.flip();
    sc.write(bufferOut);
    bufferOut.compact();
    processOut();
    updateInterestOps();
  }

}
