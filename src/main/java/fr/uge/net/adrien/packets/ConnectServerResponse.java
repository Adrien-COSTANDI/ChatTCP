package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;
import java.util.Optional;

public record ConnectServerResponse(StatusCode code) implements Packet {

  public enum StatusCode {
    OK(0),
    INVALID_PSEUDO_OR_PASSWORD(1),
    PSEUDO_ALREADY_TAKEN(2);

    public static final int BYTES = Byte.BYTES;

    private final byte value;
    StatusCode(int value) {
      this.value = (byte) value;
    }

    public static Optional<StatusCode> fromValue(byte value) {
      for (var code : StatusCode.values()) {
        if (code.value == value) {
          return Optional.of(code);
        }
      }
      return Optional.empty();
    }

    public byte value() {
      return value;
    }
  }

  @Override
  public ByteBuffer toByteBuffer() {
    var bb = ByteBuffer.allocate(Opcode.BYTES + StatusCode.BYTES);
    bb.put(Opcode.CONNECT_SERVER_RESPONSE.value())
        .put(code.value());
    return bb;
  }
}
