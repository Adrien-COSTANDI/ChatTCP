package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.DmConnect;
import fr.uge.net.adrien.readers.LongReader;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.StringReader;
import java.nio.ByteBuffer;

public class DmConnectReader implements Reader<DmConnect> {

  private final StringReader stringReader = new StringReader();
  private final LongReader longReader = new LongReader();
  private DmConnect value;
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
          state = State.DONE;
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

    var status = longReader.process(buffer);
    switch (status) {
      case DONE -> {
        value = new DmConnect(pseudo, longReader.get());
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
  public DmConnect get() {
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
    longReader.reset();
    pseudo = null;
  }

  private enum State {
    DONE, WAITING_PSEUDO, WAITING_NONCE, ERROR
  }
}
