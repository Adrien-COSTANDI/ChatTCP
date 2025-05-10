package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.ClientPublicMessage;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.StringReader;
import java.nio.ByteBuffer;

public class ClientPublicMessageReader implements Reader<ClientPublicMessage> {

  private final StringReader stringReader = new StringReader();
  private ClientPublicMessage value;
  private State state = State.WAITING;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE) {
      throw new IllegalStateException("cannot perform process if already done");
    }
    if (state == State.ERROR) {
      throw new IllegalStateException("cannot perform process if had an error");
    }
    var status = stringReader.process(buffer);
    switch (status) {
      case DONE -> {
        value = new ClientPublicMessage(stringReader.get());
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
  public ClientPublicMessage get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    value = null;
    state = State.WAITING;
    stringReader.reset();
  }

  private enum State {
    DONE, WAITING, ERROR
  }
}
