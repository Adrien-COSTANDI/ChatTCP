package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record ConnectNoAuth(String pseudo) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbPseudo = Packet.CHARSET.encode(pseudo);
    var bb = ByteBuffer.allocate(Opcode.BYTES + Short.BYTES + bbPseudo.remaining());
    bb.put(Opcode.CONNECT_NO_AUTH.value())
        .putShort((short) bbPseudo.remaining())
        .put(bbPseudo);
    return bb;
  }
}
