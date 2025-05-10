package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LongReaderTest {

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);

    var longReader = new LongReader();
    assertEquals(Reader.ProcessStatus.REFILL, longReader.process(buffer));

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = 1234;
    buffer.putLong(value);

    var longReader = new LongReader();
    assertEquals(Reader.ProcessStatus.DONE, longReader.process(buffer));
    assertEquals(value, longReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.putLong(2);
    buffer.limit(6); // not the size of a long

    var longReader = new LongReader();
    assertEquals(Reader.ProcessStatus.REFILL, longReader.process(buffer));

    buffer.clear();
    buffer.putLong(2);
    buffer.position(6);
    buffer.compact();
    buffer.limit(2);
    assertEquals(Reader.ProcessStatus.DONE, longReader.process(buffer));
    assertEquals(2, longReader.get());
  }

  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.putLong(1);

    var longReader = new LongReader();
    assertEquals(Reader.ProcessStatus.DONE, longReader.process(buffer));
    assertThrows(IllegalStateException.class, () -> longReader.process(buffer));
  }

  @Test
  public void errorGet() {
    var longReader = new LongReader();
    assertThrows(IllegalStateException.class, () -> longReader.get());
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = 1234;
    var value2 = 4321;
    buffer.putLong(value1);
    buffer.putLong(value2);

    var longReader = new LongReader();
    assertEquals(Reader.ProcessStatus.DONE, longReader.process(buffer));
    assertEquals(value1, longReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(Long.BYTES, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());

    longReader.reset();

    assertEquals(Reader.ProcessStatus.DONE, longReader.process(buffer));
    assertEquals(value2, longReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}