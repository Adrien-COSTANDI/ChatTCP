package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.DmFileHeader;
import fr.uge.net.adrien.readers.IntReader;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.StringReader;
import java.nio.ByteBuffer;

public final class DmFileHeaderReader implements Reader<DmFileHeader> {

  private DmFileHeader value;
  private State state = State.WAITING_FILENAME;
  private String filename;
  private final StringReader stringReader = new StringReader();
  private final IntReader intReader = new IntReader();

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    if (state == State.DONE) {
      throw new IllegalStateException("cannot perform process if already done");
    }
    if (state == State.ERROR) {
      throw new IllegalStateException("cannot perform process if had an error");
    }
    if (state == State.WAITING_FILENAME) {
      var status = stringReader.process(buffer);
      switch (status) {
        case DONE -> {
          filename = stringReader.get();
          state = State.WAITING_SIZE;
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

    var status = intReader.process(buffer);

    switch (status) {
      case DONE -> {
        var size = intReader.get();
        value = new DmFileHeader(filename, size);
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
  public DmFileHeader get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    value = null;
    state = State.WAITING_FILENAME;
    stringReader.reset();
    intReader.reset();
  }

  private enum State {
    DONE, WAITING_FILENAME, WAITING_SIZE, ERROR
  }
}
