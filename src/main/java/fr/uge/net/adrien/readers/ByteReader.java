package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;

public class ByteReader implements Reader<Byte> {

  private enum State {
    DONE, WAITING, ERROR
  }

  private State state = State.WAITING;
  private byte value = -1;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }
    buffer.flip();
    if (buffer.remaining() > 0) {
      value = buffer.get();
      buffer.compact();
      state = State.DONE;
      return ProcessStatus.DONE;
    }
    buffer.compact();
    state = State.WAITING;
    return ProcessStatus.REFILL;
  }

  @Override
  public Byte get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return value;
  }

  @Override
  public void reset() {
    state = State.WAITING;
    value = -1;
  }
}