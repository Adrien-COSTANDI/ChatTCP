package fr.uge.net.adrien.readers;

import fr.uge.net.adrien.packets.common.Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class AddressReader implements Reader<Address> {

  private enum State {
    DONE, WAITING_ADDRESS, WAITING_PORT, ERROR
  }

  private final InetAddressReader inetAddressReader = new InetAddressReader();
  private final IntReader intReader = new IntReader();
  private State state = State.WAITING_ADDRESS;
  private InetAddress inetAddress = null;
  private Address address = null;
  private int port = -1;

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    switch (state) {
      case ERROR -> throw new IllegalStateException("cannot perform process if had an error");
      case DONE ->
          throw new IllegalStateException("cannot perform process if not reset and already done");
      case WAITING_ADDRESS -> {
        var status = inetAddressReader.process(buffer);

        switch (status) {
          case DONE -> {
            inetAddress = inetAddressReader.get();
            inetAddressReader.reset();
            state = State.WAITING_PORT;
          }
          case REFILL -> {
            return ProcessStatus.REFILL;
          }
          case ERROR -> {
            state = State.ERROR;
            return ProcessStatus.ERROR;
          }
        }
      }
    }

    var status = intReader.process(buffer);
    switch (status) {
      case DONE -> {
        port = intReader.get();
        intReader.reset();
        if (port < 1024 || port > 65535) {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
      }
      case REFILL -> {
        return ProcessStatus.REFILL;
      }
      case ERROR -> {
        state = State.ERROR;
        return ProcessStatus.ERROR;
      }
    }
    var inetSocketAddress = new InetSocketAddress(inetAddress, port);
    address = new Address(inetSocketAddress);
    state = State.DONE;
    return ProcessStatus.DONE;
  }

  @Override
  public Address get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return address;
  }

  @Override
  public void reset() {
    state = State.WAITING_ADDRESS;
    intReader.reset();
    inetAddressReader.reset();
    port = -1;
    inetAddress = null;
    address = null;
  }
}
