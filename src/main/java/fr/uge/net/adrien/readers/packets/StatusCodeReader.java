package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.readers.ByteReader;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;

public class StatusCodeReader implements Reader<ConnectServerResponse.StatusCode> {

  private final ByteReader byteReader = new ByteReader();
  private State state = State.WAITING;
  private ConnectServerResponse.StatusCode value;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE) {
      throw new IllegalStateException("cannot perform process if already done");
    }
    if (state == State.ERROR) {
      throw new IllegalStateException("cannot perform process if had an error");
    }

    var status = byteReader.process(buffer);

    switch (status) {
      case DONE -> {
        var opt = ConnectServerResponse.StatusCode.fromValue(byteReader.get());
        if (opt.isEmpty()) {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        value = opt.get();
        state = State.DONE;
        return ProcessStatus.DONE;
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

  @Override
  public ConnectServerResponse.StatusCode get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    value = null;
    byteReader.reset();
    state = State.WAITING;
  }

  private enum State {
    DONE, WAITING, ERROR
  }
}
