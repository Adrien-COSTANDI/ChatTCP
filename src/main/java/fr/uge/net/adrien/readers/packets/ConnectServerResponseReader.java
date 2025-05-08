package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;

public class ConnectServerResponseReader implements Reader<ConnectServerResponse> {

  private final StatusCodeReader statusCodeReader = new StatusCodeReader();
  private ConnectServerResponse value;
  private State state = State.WAITING;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE) {
      throw new IllegalStateException("cannot perform process if already done");
    }
    if (state == State.ERROR) {
      throw new IllegalStateException("cannot perform process if had an error");
    }
    var status = statusCodeReader.process(buffer);

    switch (status) {
      case DONE -> {
        value = new ConnectServerResponse(statusCodeReader.get());
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
  public ConnectServerResponse get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    value = null;
    state = State.WAITING;
    statusCodeReader.reset();
  }

  private enum State {
    DONE, WAITING, ERROR
  }
}
