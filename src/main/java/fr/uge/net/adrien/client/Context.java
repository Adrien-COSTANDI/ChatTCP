package fr.uge.net.adrien.client;

import fr.uge.net.adrien.packets.ConnectAuth;
import fr.uge.net.adrien.packets.ConnectNoAuth;
import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.packets.PacketReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

class Context {

  public static final int BUFFER_SIZE = 1024;
  private final SelectionKey key;
  private final SocketChannel sc;
  private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
  private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
  private final ArrayDeque<Packet> queue = new ArrayDeque<>();
  private final PacketReader packetReader = new PacketReader();
  private boolean closed = false;

  private final Client client;

  public Context(SelectionKey key, Client client) {
    this.key = key;
    this.sc = (SocketChannel) key.channel();
    this.client = client;
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
      silentlyClose();
    }
  }

  private void processReceivedPacket(Packet packet) {
    System.out.println("received " + packet);
    switch (packet) {
      case ConnectNoAuth connectNoAuth -> {
      }
      case ConnectAuth connectAuth -> {
      }
      case ConnectServerResponse connectServerResponse -> {
        switch (connectServerResponse.code()) {
          case OK -> System.out.println("connected");
          case PSEUDO_ALREADY_TAKEN -> {
            System.out.println("pseudo already taken");
            silentlyClose();
            client.shutdown();
          }
          case INVALID_PSEUDO_OR_PASSWORD -> {
            System.out.println("invalid pseudo or password");
            silentlyClose();
            client.shutdown();
          }
        }
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
   * Try to fill bufferOut from the queue
   */
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
   * updateInterestOps and after the call. Also it is assumed that process has
   * been called just before updateInterestOps.
   */
  private void updateInterestOps() {
    var interestOps = 0;

    if (bufferOut.position() > 0) {
      interestOps |= SelectionKey.OP_WRITE;
    }

    if (!closed && bufferIn.hasRemaining()) {
      interestOps |= SelectionKey.OP_READ;
    }

    if (interestOps == 0) {
      silentlyClose();
      return;
    }

    key.interestOps(interestOps);
  }

  private void silentlyClose() {
    try {
      sc.close();
      closed = true;
    } catch (IOException e) {
      // ignore exception
    }
  }

  /**
   * Performs the read action on sc.
   * <p>
   * The convention is that both buffers are in write-mode before the call to
   * doRead and after the call
   *
   * @throws IOException
   */
  void doRead() throws IOException {
    if (sc.read(bufferIn) == -1) {
      closed = true;
    }
    processIn();
    updateInterestOps();
  }

  /**
   * Performs the write action on sc.
   * <p>
   * The convention is that both buffers are in write-mode before the call to
   * doWrite and after the call
   *
   * @throws IOException if an I/O error occurs during the write operation on the SocketChannel
   */
  void doWrite() throws IOException {
    bufferOut.flip();
    sc.write(bufferOut);
    bufferOut.compact();
    processOut();
    updateInterestOps();
  }

  void doConnect() throws IOException {
    if (!sc.finishConnect()) {
      return; // the selector gave a bad hint
    }
    key.interestOps(SelectionKey.OP_WRITE);
    System.out.println("my adress : " + sc.getLocalAddress());

    if (client.password().isPresent()) {
      send(new ConnectAuth(client.pseudo(), client.password().orElseThrow()));
    } else {
      send(new ConnectNoAuth(client.pseudo()));
    }
  }
}
