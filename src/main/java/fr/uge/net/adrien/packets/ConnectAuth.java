package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record ConnectAuth(String pseudo, String password) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbPseudo = Packet.CHARSET.encode(pseudo);
    var bbPassword = Packet.CHARSET.encode(password);

    var bb = ByteBuffer.allocate(
        Opcode.BYTES + Short.BYTES + bbPseudo.remaining() + Short.BYTES + bbPassword.remaining());
    bb.put(Opcode.CONNECT_AUTH.value())
        .putShort((short) bbPseudo.remaining())
        .put(bbPseudo)
        .putShort((short) bbPassword.remaining())
        .put(bbPassword);
    return bb;
  }
}
