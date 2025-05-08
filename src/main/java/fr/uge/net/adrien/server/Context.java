package fr.uge.net.adrien.server;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;

class Context {

  private final ByteBuffer bufferIn = ByteBuffer.allocate(Server.BUFFER_SIZE);
  private final ByteBuffer bufferOut = ByteBuffer.allocate(Server.BUFFER_SIZE);

  private boolean closed = false;

  private final SelectionKey key;
  private final SocketChannel sc;
  private final Server server;

  public Context(SelectionKey key, Server server) {
    this.key = Objects.requireNonNull(key);
    this.sc = (SocketChannel) key.channel();
    this.server = Objects.requireNonNull(server);
  }

  private void processIn() {

  }

  private void processOut() {
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
   * @throws IOException
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
   * @throws IOException
   */
  public void doWrite() throws IOException {
    bufferOut.flip();
    sc.write(bufferOut);
    bufferOut.compact();
    processOut();
    updateInterestOps();
  }

}
