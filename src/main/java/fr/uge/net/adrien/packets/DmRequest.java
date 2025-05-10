package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record DmRequest(String pseudo) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbTarget = Packet.CHARSET.encode(pseudo);
    var bb = ByteBuffer.allocate(Opcode.BYTES + Short.BYTES + bbTarget.remaining());
    bb.put(Opcode.DM_REQUEST.value()).putShort((short) bbTarget.remaining()).put(bbTarget);
    return bb;
  }
}
