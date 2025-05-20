package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.DmFileContent;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmFileContentReaderTest {

  private static void checkOpCode(ByteBuffer buffer) {
    buffer.flip();
    assertEquals(Packet.Opcode.DM_FILE_CONTENT.value(), buffer.get());
    buffer.compact();
  }

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);
    var reader = new DmFileContentReader();

    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // Verify buffer position and limit
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new DmFileContent("Hello".getBytes(StandardCharsets.UTF_8));
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    var reader = new DmFileContentReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertArrayEquals(value.contenu(), reader.get().contenu());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new DmFileContent("Hello".getBytes(StandardCharsets.UTF_8));
    buffer.put(value.toByteBuffer().flip());
    buffer.limit(0); // not the size of a byte

    var reader = new DmFileContentReader();
    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    buffer = ByteBuffer.allocate(1024);
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertArrayEquals(value.contenu(), reader.get().contenu());
  }

  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new DmFileContent("Hello".getBytes(StandardCharsets.UTF_8));
    buffer.put(value.toByteBuffer().flip());
    checkOpCode(buffer);

    var reader = new DmFileContentReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new DmFileContentReader();
    assertThrows(IllegalStateException.class, reader::get);
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = new DmFileContent("Hello".getBytes(StandardCharsets.UTF_8));
    var value2 = new DmFileContent("Papepipopu".getBytes(StandardCharsets.UTF_8));
    buffer.put(value1.toByteBuffer().flip());
    buffer.put(value2.toByteBuffer().flip());

    var reader = new DmFileContentReader();
    checkOpCode(buffer);
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertArrayEquals(value1.contenu(), reader.get().contenu());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(value2.length(), buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());

    reader.reset();
    checkOpCode(buffer);

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertArrayEquals(value2.contenu(), reader.get().contenu());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}