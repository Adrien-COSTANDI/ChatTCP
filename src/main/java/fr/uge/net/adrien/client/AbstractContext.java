package fr.uge.net.adrien.client;

import fr.uge.net.adrien.Context;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.packets.PacketReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;

abstract class AbstractContext implements Context {

  protected static final int BUFFER_SIZE = 32000;

  protected final SelectionKey key;
  protected final SocketChannel sc;
  protected final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
  protected final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
  protected final ArrayDeque<Packet> queue = new ArrayDeque<>();
  private final PacketReader packetReader = new PacketReader();

  protected boolean closed = false;

  protected AbstractContext(SelectionKey key) {
    this.key = Objects.requireNonNull(key);
    this.sc = (SocketChannel) key.channel();
  }

  abstract protected void processReceivedPacket(Packet packet);

  public void send(Packet packet) {
    try {
      System.out.println("sending " + packet + " to " + sc.getRemoteAddress().toString());
    } catch (IOException e) {
      System.out.println("sending " + packet);
    }
    queue.add(packet);
    processOut();
    updateInterestOps();
  }

  protected void processOut() {
    while (!queue.isEmpty() && bufferOut.remaining() >= queue.peek().length()) {
      var packetBuffer = queue.poll().toByteBuffer().flip();
      bufferOut.put(packetBuffer);
    }
  }

  protected void updateInterestOps() {
    var interestOps = 0;

    if (bufferOut.position() > 0) {
      interestOps |= SelectionKey.OP_WRITE;
    }

    if (!closed && bufferIn.hasRemaining()) {
      interestOps |= SelectionKey.OP_READ;
    }

    if (interestOps == 0) {
      close();
      return;
    }

    key.interestOps(interestOps);
  }

  @Override
  public void close() {
    try {
      sc.close();
      closed = true;
    } catch (IOException e) {
      // ignore
    }
  }

  protected void processIn() {
    Reader.ProcessStatus status;

    while ((status = packetReader.process(bufferIn)) == Reader.ProcessStatus.DONE) {
      var packet = packetReader.get();
      packetReader.reset();
      processReceivedPacket(packet);
    }

    if (status == Reader.ProcessStatus.ERROR) {
      close();
    }
  }

  @Override
  public void doRead() throws IOException {
    if (sc.read(bufferIn) == -1) {
      closed = true;
    }
    processIn();
    updateInterestOps();
  }

  @Override
  public void doWrite() throws IOException {
    bufferOut.flip();
    sc.write(bufferOut);
    bufferOut.compact();
    processOut();
    updateInterestOps();
  }
}
