package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record ServerForwardPublicMessage(String contenu, String pseudo) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbContenu = Packet.CHARSET.encode(contenu);
    var bbPseudo = Packet.CHARSET.encode(pseudo);

    var bb = ByteBuffer.allocate(
        Opcode.BYTES + Short.BYTES + bbPseudo.remaining() + Short.BYTES + bbContenu.remaining());
    bb.put(Opcode.SERVER_FORWARD_PUBLIC_MESSAGE.value())
        .putShort((short) bbContenu.remaining())
        .put(bbContenu)
        .putShort((short) bbPseudo.remaining())
        .put(bbPseudo);
    return bb;
  }
}
