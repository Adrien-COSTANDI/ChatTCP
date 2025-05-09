package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;

public class PacketReader implements Reader<Packet> {

  private Packet packet;
  private final OpCodeReader opcodeReader = new OpCodeReader();
  private State state = State.WAITING_OPCODE;
  private Reader<? extends Packet> payloadReader;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.ERROR) {
      throw new IllegalStateException("cannot perform process if had an error");
    }
    if (state == State.DONE) {
      throw new IllegalStateException("cannot perform process if already done");
    }
    if (state == State.WAITING_OPCODE) {
      var status = opcodeReader.process(buffer);

      switch (status) {
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case DONE -> {
          var opcode = opcodeReader.get();
          opcodeReader.reset();
          state = State.WAITING_PAYLOAD;

          payloadReader = switch (opcode) {
            case CONNECT_NO_AUTH -> new ConnectNoAuthReader();
            case CONNECT_AUTH -> new ConnectAuthReader();
            case CONNECT_SERVER_RESPONSE -> new ConnectServerResponseReader();
          };
        }
      }
    }

    var status = payloadReader.process(buffer);

    switch (status) {
      case REFILL -> {
        return ProcessStatus.REFILL;
      }
      case DONE -> {
        packet = payloadReader.get();
        state = State.DONE;
        payloadReader.reset();
        return ProcessStatus.DONE;
      }
      default -> {
        state = State.ERROR;
        return ProcessStatus.ERROR;
      }
    }
  }

  @Override
  public Packet get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return packet;
  }

  @Override
  public void reset() {
    opcodeReader.reset();
    packet = null;
    state = State.WAITING_OPCODE;
    payloadReader = null;
  }

  private enum State {
    DONE, WAITING_OPCODE, WAITING_PAYLOAD, ERROR
  }
}
