package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record ClientPublicMessage(String contenu) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbPseudo = Packet.CHARSET.encode(contenu);
    var bb = ByteBuffer.allocate(Opcode.BYTES + Short.BYTES + bbPseudo.remaining());
    bb.put(Opcode.CLIENT_PUBLIC_MESSAGE.value())
        .putShort((short) bbPseudo.remaining())
        .put(bbPseudo);
    return bb;
  }
}
