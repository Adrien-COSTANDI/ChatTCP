package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.ByteReader;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;

class OpCodeReader implements Reader<Packet.Opcode> {

  private final ByteReader byteReader = new ByteReader();
  private Packet.Opcode value;
  private State state = State.WAITING;

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
        var opt = Packet.Opcode.fromValue(byteReader.get());
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
  public Packet.Opcode get() {
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
