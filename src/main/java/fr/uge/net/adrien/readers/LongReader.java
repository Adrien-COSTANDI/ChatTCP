package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;

public class LongReader implements Reader<Long> {

  private enum State {
    DONE, WAITING, ERROR
  }

  private State state = State.WAITING;
  private final ByteBuffer internalBuffer = ByteBuffer.allocate(Long.BYTES); // write-mode
  private long value;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }
    buffer.flip();
    try {
      if (buffer.remaining() <= internalBuffer.remaining()) {
        internalBuffer.put(buffer);
      } else {
        var oldLimit = buffer.limit();
        buffer.limit(internalBuffer.remaining());
        internalBuffer.put(buffer);
        buffer.limit(oldLimit);
      }
    } finally {
      buffer.compact();
    }
    if (internalBuffer.hasRemaining()) {
      return ProcessStatus.REFILL;
    }
    state = State.DONE;
    internalBuffer.flip();
    value = internalBuffer.getLong();
    return ProcessStatus.DONE;
  }

  @Override
  public Long get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return value;
  }

  @Override
  public void reset() {
    state = State.WAITING;
    internalBuffer.clear();
  }
}