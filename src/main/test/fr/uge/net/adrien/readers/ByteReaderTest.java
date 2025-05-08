package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ByteReaderTest {

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);

    ByteReader byteReader = new ByteReader();
    assertEquals(Reader.ProcessStatus.REFILL, byteReader.process(buffer));

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = (byte) 12;
    buffer.put(value);

    ByteReader byteReader = new ByteReader();
    assertEquals(Reader.ProcessStatus.DONE, byteReader.process(buffer));
    assertEquals(value, byteReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) 1);
    buffer.limit(0); // not the size of a byte

    var byteReader = new ByteReader();
    assertEquals(Reader.ProcessStatus.REFILL, byteReader.process(buffer));

    buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) 2);
    assertEquals(Reader.ProcessStatus.DONE, byteReader.process(buffer));
    assertEquals((byte) 2, byteReader.get());
  }

  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) 1);

    ByteReader byteReader = new ByteReader();
    assertEquals(Reader.ProcessStatus.DONE, byteReader.process(buffer));
    assertThrows(IllegalStateException.class, () -> byteReader.process(buffer));
  }

  @Test
  public void errorGet() {
    var byteReader = new ByteReader();
    assertThrows(IllegalStateException.class, () -> {
      var res = byteReader.get();
    });
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = (byte) 12;
    var value2 = (byte) 21;
    buffer.put(value1);
    buffer.put(value2);

    ByteReader byteReader = new ByteReader();
    assertEquals(Reader.ProcessStatus.DONE, byteReader.process(buffer));
    assertEquals(value1, byteReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(Byte.BYTES, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());

    byteReader.reset();

    assertEquals(Reader.ProcessStatus.DONE, byteReader.process(buffer));
    assertEquals(value2, byteReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}