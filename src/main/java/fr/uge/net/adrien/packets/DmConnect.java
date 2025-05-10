package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record DmConnect(String pseudo, long nonce) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbEmitter = Packet.CHARSET.encode(pseudo);
    var bb = ByteBuffer.allocate(Opcode.BYTES + Short.BYTES + bbEmitter.remaining() + Long.BYTES);
    bb.put(Opcode.DM_CONNECT.value())
        .putShort((short) bbEmitter.remaining())
        .put(bbEmitter)
        .putLong(nonce);
    return bb;
  }
}
