package fr.uge.net.adrien.packets;

import fr.uge.net.adrien.packets.common.Address;
import java.nio.ByteBuffer;
import java.util.Optional;

public record DmResponse(String pseudo,
                         Response ok,
                         Optional<Long> nonce,
                         Optional<Address> address) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbEmitter = Packet.CHARSET.encode(pseudo);
    ByteBuffer bb = ByteBuffer.allocate(
        Opcode.BYTES + Short.BYTES + bbEmitter.remaining() + Byte.BYTES + Long.BYTES +
        Address.MAX_BYTES);
    bb.put(Opcode.DM_RESPONSE.value())
        .putShort((short) bbEmitter.remaining())
        .put(bbEmitter)
        .put(ok.value);

    if (ok == Response.NO) {
      return bb;
    }
    bb.putLong(nonce.orElseThrow(IllegalStateException::new))
        .put(address.orElseThrow(IllegalStateException::new).toByteBuffer().flip());
    return bb;
  }

  public enum Response {
    YES(1), NO(0);

    private final byte value;

    Response(int i) {
      this.value = (byte) i;
    }

    public static Optional<Response> fromValue(Byte aByte) {
      for (var response : Response.values()) {
        if (response.value == aByte) {
          return Optional.of(response);
        }
      }
      return Optional.empty();
    }

    public byte value() {
      return value;
    }
  }
}
