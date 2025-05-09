package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.ConnectAuth;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.StringReader;
import java.nio.ByteBuffer;

public class ConnectAuthReader implements Reader<ConnectAuth> {

  private final StringReader stringReader = new StringReader();
  private ConnectAuth value;
  private State state = State.WAITING_PSEUDO;

  private String pseudo;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE) {
      throw new IllegalStateException("cannot perform process if already done");
    }
    if (state == State.ERROR) {
      throw new IllegalStateException("cannot perform process if had an error");
    }

    if (state == State.WAITING_PSEUDO) {
      var status = stringReader.process(buffer);
      switch (status) {
        case DONE -> {
          pseudo = stringReader.get();
          stringReader.reset();
          state = State.WAITING_PASSWORD;
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

    var status = stringReader.process(buffer);

    switch (status) {
      case REFILL -> {
        return ProcessStatus.REFILL;
      }
      case DONE -> {
        value = new ConnectAuth(pseudo, stringReader.get());
        state = State.DONE;
        stringReader.reset();
        return ProcessStatus.DONE;
      }
      default -> {
        state = State.ERROR;
        return ProcessStatus.ERROR;
      }
    }
  }

  @Override
  public ConnectAuth get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    value = null;
    state = State.WAITING_PSEUDO;
    stringReader.reset();
  }

  private enum State {
    DONE, WAITING_PSEUDO, WAITING_PASSWORD, ERROR
  }
}
