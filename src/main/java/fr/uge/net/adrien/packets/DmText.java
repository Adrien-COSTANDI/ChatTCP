package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record DmText(String contenu) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbContenu = Packet.CHARSET.encode(contenu);
    var bb = ByteBuffer.allocate(Opcode.BYTES + Short.BYTES + bbContenu.remaining());
    bb.put(Opcode.DM_TEXT.value()).putShort((short) bbContenu.remaining()).put(bbContenu);
    return bb;
  }
}
