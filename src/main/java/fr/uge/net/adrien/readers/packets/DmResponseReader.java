package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.DmResponse;
import fr.uge.net.adrien.readers.AddressReader;
import fr.uge.net.adrien.readers.LongReader;
import fr.uge.net.adrien.readers.Reader;
import fr.uge.net.adrien.readers.StringReader;
import java.nio.ByteBuffer;
import java.util.Optional;

public class DmResponseReader implements Reader<DmResponse> {

  private final StringReader stringReader = new StringReader();
  private final ResponseReader responseReader = new ResponseReader();
  private final LongReader longReader = new LongReader();
  private final AddressReader addressReader = new AddressReader();

  private DmResponse value;
  private State state = State.WAITING_PSEUDO;
  private DmResponse.Response response;
  private Long nonce;
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
          state = State.WAITING_RESPONSE;
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
    if (state == State.WAITING_RESPONSE) {
      var status = responseReader.process(buffer);
      switch (status) {
        case DONE -> {
          response = responseReader.get();
          if (response == DmResponse.Response.NO) {
            state = State.DONE;
            value = new DmResponse(pseudo, response, Optional.empty(), Optional.empty());
            return ProcessStatus.DONE;
          }
          state = State.WAITING_NONCE;
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
    if (state == State.WAITING_NONCE) {
      var status = longReader.process(buffer);
      switch (status) {
        case DONE -> {
          nonce = longReader.get();
          state = State.WAITING_ADDRESS;
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

    var status = addressReader.process(buffer);

    switch (status) {
      case DONE -> {
        value = new DmResponse(pseudo,
                               response,
                               Optional.ofNullable(nonce),
                               Optional.ofNullable(addressReader.get()));
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
  public DmResponse get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    value = null;
    nonce = null;
    state = State.WAITING_PSEUDO;
    stringReader.reset();
    responseReader.reset();
    longReader.reset();
    addressReader.reset();
  }

  private enum State {
    DONE, WAITING_PSEUDO, WAITING_RESPONSE, WAITING_NONCE, WAITING_ADDRESS, ERROR
  }
}
