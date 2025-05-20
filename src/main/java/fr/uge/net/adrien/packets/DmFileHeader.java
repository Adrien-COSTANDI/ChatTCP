package fr.uge.net.adrien.packets;

import java.nio.ByteBuffer;

public record DmFileHeader(String fileName, int size) implements Packet {

  @Override
  public ByteBuffer toByteBuffer() {
    var bbFileName = Packet.CHARSET.encode(fileName);

    var bb =
        ByteBuffer.allocate(Opcode.BYTES + Short.BYTES + bbFileName.remaining() + Integer.BYTES);
    bb.put(Opcode.DM_FILE_HEADER.value())
        .putShort((short) bbFileName.remaining())
        .put(bbFileName)
        .putInt(size);
    return bb;
  }
}
