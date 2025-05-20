package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.DmFileContent;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.ShortReader;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DmFileContentReader implements Reader<DmFileContent> {

  private State state = State.WAITING_SIZE;
  private DmFileContent value;
  private short size = -1;
  private final ByteBuffer contentBuffer = ByteBuffer.allocate(8192);
  private final ShortReader shortReader = new ShortReader();

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE) {
      throw new IllegalStateException("cannot perform process if already done");
    }
    if (state == State.ERROR) {
      throw new IllegalStateException("cannot perform process if had an error");
    }
    if (state == State.WAITING_SIZE) {
      var status = shortReader.process(buffer);
      switch (status) {
        case DONE -> {
          size = shortReader.get();
          state = State.WAITING_CONTENT;
          contentBuffer.limit(size);
        }
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
        default -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
    }

    buffer.flip(); // read mode
    try {
      if (buffer.remaining() <= contentBuffer.remaining()) {
        contentBuffer.put(buffer);
      } else {
        var oldLimit = buffer.limit();
        buffer.limit(contentBuffer.remaining());
        contentBuffer.put(buffer);
        buffer.limit(oldLimit);
      }
    } finally {
      buffer.compact();
    }

    if (contentBuffer.hasRemaining()) {
      return ProcessStatus.REFILL;
    }

    value = new DmFileContent(Arrays.copyOf(contentBuffer.array(), size));
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  @Override
  public DmFileContent get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    value = null;
    contentBuffer.clear();
    size = -1;
    state = State.WAITING_SIZE;
    shortReader.reset();
  }

  private enum State {
    DONE, WAITING_SIZE, WAITING_CONTENT, ERROR
  }
}
