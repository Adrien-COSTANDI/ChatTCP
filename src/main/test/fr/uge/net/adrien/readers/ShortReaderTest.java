package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShortReaderTest {

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);

    var reader = new ShortReader();
    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = (short) 1234;
    buffer.putShort(value);

    var reader = new ShortReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.putShort((short) 2);
    buffer.limit(1); // not the size of a short

    var reader = new ShortReader();
    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    buffer.clear();
    buffer.putShort((short) 2);
    buffer.position(1);
    buffer.compact();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals((short) 2, reader.get());
  }

  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.putShort((short) 1);

    var reader = new ShortReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new ShortReader();
    assertThrows(IllegalStateException.class, () -> reader.get());
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = (short) 1234;
    var value2 = (short) 4321;
    buffer.putShort(value1);
    buffer.putShort(value2);

    var reader = new ShortReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value1, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(Short.BYTES, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());

    reader.reset();

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value2, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}