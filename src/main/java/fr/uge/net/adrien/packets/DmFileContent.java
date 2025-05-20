package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record DmFileContent(byte[] contenu) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bb = ByteBuffer.allocate(Opcode.BYTES + Short.BYTES + contenu.length);
    bb.put(Opcode.DM_FILE_CONTENT.value()).putShort((short) contenu.length).put(contenu);
    return bb;
  }
}
