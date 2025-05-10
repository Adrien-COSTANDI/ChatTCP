package fr.uge.net.adrien.readers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class InetAddressReader implements Reader<InetAddress> {

  private enum State {
    DONE, WAITING_IP_VERSION, WAITING_ADDRESS, ERROR
  }


  private final ByteBuffer rawAddress = ByteBuffer.allocate(17); // write-mode
  private State state = State.WAITING_IP_VERSION;
  private final ByteReader byteReader = new ByteReader();
  private byte ipVersion = -1; // 0 = IPV4, 1 = IPV6
  private InetAddress value;

  private int sizeAddress() {
    return switch (ipVersion) {
      case 0 -> 4;
      case 1 -> 16;
      default -> throw new AssertionError("IP version not found");
    };
  }

  @Override
  public ProcessStatus process(ByteBuffer buffer) {
    switch (state) {
      case ERROR -> throw new IllegalStateException("cannot perform process if had an error");
      case DONE ->
          throw new IllegalStateException("cannot perform process if not reset and already done");
      case WAITING_IP_VERSION -> {
        var status = byteReader.process(buffer);

        switch (status) {
          case DONE -> {
            ipVersion = byteReader.get();
            if (ipVersion != 0 && ipVersion != 1) {
              state = State.ERROR;
              return ProcessStatus.ERROR;
            }
            // internalBuffer.limit(sizeAddress()); // IMPORTANT POUR LA CONDITION (buffer.remaining() <= internalBuffer.remaining())
            rawAddress.limit(sizeAddress());
            byteReader.reset();
            state = State.WAITING_ADDRESS;
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

    buffer.flip();
    try {
      if (buffer.remaining() <= rawAddress.remaining()) {
        rawAddress.put(buffer);
      } else {
        var oldLimit = buffer.limit();
        buffer.limit(rawAddress.remaining());
        rawAddress.put(buffer);
        buffer.limit(oldLimit);
      }
    } finally {
      buffer.compact();
    }
    if (rawAddress.hasRemaining()) {
      return ProcessStatus.REFILL;
    }
    state = State.DONE;
    rawAddress.flip();
    try {
      value = InetAddress.getByAddress(Arrays.copyOf(rawAddress.array(), sizeAddress()));
    } catch (UnknownHostException e) {
      throw new AssertionError(e);
    }
    return ProcessStatus.DONE;
  }

  @Override
  public InetAddress get() {
    if (state != State.DONE) {
      throw new IllegalStateException("cannot get the value if process didn't finished");
    }
    return value;
  }

  @Override
  public void reset() {
    state = State.WAITING_IP_VERSION;
    byteReader.reset();
    ipVersion = -1;
    value = null;
    rawAddress.clear();
  }
}
