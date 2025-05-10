package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Represents a generic Packet interface that can be converted into a ByteBuffer.
 * The interface also defines a nested {@code Opcode} enumeration for various packet types.
 */
public sealed interface Packet
    permits ConnectNoAuth, ConnectAuth, ConnectServerResponse, ClientPublicMessage,
    ServerForwardPublicMessage/*, DmRequest,
DmResponse, DmConnect, DmText, DmFileHeader, DmFile*/ {

  Charset CHARSET = StandardCharsets.UTF_8;

  /**
   * Convert a packet to a ByteBuffer.
   * The returned ByteBuffer is in write mode.
   *
   * @return a ByteBuffer containing the packet
   */
  ByteBuffer toByteBuffer();

  default int length() {
    return toByteBuffer().flip().remaining();
  }

  enum Opcode {

    CONNECT_NO_AUTH(1),
    CONNECT_AUTH(2),
    CONNECT_SERVER_RESPONSE(3), CLIENT_PUBLIC_MESSAGE(4),
    SERVER_FORWARD_PUBLIC_MESSAGE(5),
    /*DM_REQUEST(6),
    DM_RESPONSE(7),
    DM_CONNECT(8),
    DM_TEXT(9),
    DM_FILE_HEADER(10),
    DM_FILE_CONTENT(11)*/;

    private final byte value;

    Opcode(int b) {
      this.value = (byte) b;
    }

    public static final int BYTES = Byte.BYTES;

    public static Optional<Opcode> fromValue(Byte aByte) {
      for (var opcode : Opcode.values()) {
        if (opcode.value == aByte) {
          return Optional.of(opcode);
        }
      }
      return Optional.empty();
    }

    public byte value() {
      return value;
    }
  }

}
