package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.DmConnect;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmConnectReaderTest {

  private static void checkOpCode(ByteBuffer buffer) {
    buffer.flip();
    assertEquals(Packet.Opcode.DM_CONNECT.value(), buffer.get());
    buffer.compact();
  }

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);
    var reader = new DmConnectReader();

    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // Verify buffer position and limit
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new DmConnect("Bob", 1234L);
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    var reader = new DmConnectReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new DmConnect("Bob", 1234L);
    buffer.put(value.toByteBuffer().flip());
    buffer.limit(0); // not the size of a byte

    var reader = new DmConnectReader();
    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    buffer = ByteBuffer.allocate(1024);
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());
  }

  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new DmConnect("Bob", 1234L);
    buffer.put(value.toByteBuffer().flip());
    checkOpCode(buffer);

    var reader = new DmConnectReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new DmConnectReader();
    assertThrows(IllegalStateException.class, reader::get);
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = new DmConnect("Bob", 1234L);
    var value2 = new DmConnect("Gertrude", 1234L);
    buffer.put(value1.toByteBuffer().flip());
    buffer.put(value2.toByteBuffer().flip());

    var reader = new DmConnectReader();
    checkOpCode(buffer);
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value1, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(value2.length(), buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());

    reader.reset();
    checkOpCode(buffer);

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value2, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}