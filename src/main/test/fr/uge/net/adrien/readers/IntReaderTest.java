package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntReaderTest {

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);

    var intReader = new IntReader();
    assertEquals(Reader.ProcessStatus.REFILL, intReader.process(buffer));

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = 1234;
    buffer.putInt(value);

    var intReader = new IntReader();
    assertEquals(Reader.ProcessStatus.DONE, intReader.process(buffer));
    assertEquals(value, intReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.putInt(2);
    buffer.limit(2); // not the size of an int

    var intReader = new IntReader();
    assertEquals(Reader.ProcessStatus.REFILL, intReader.process(buffer));

    buffer.clear();
    buffer.putInt(2);
    buffer.position(2);
    buffer.compact();
    buffer.limit(2);
    assertEquals(Reader.ProcessStatus.DONE, intReader.process(buffer));
    assertEquals(2, intReader.get());
  }

  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.putInt(1);

    var intReader = new IntReader();
    assertEquals(Reader.ProcessStatus.DONE, intReader.process(buffer));
    assertThrows(IllegalStateException.class, () -> intReader.process(buffer));
  }

  @Test
  public void errorGet() {
    var intReader = new IntReader();
    assertThrows(IllegalStateException.class, () -> intReader.get());
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = 1234;
    var value2 = 4321;
    buffer.putInt(value1);
    buffer.putInt(value2);

    var intReader = new IntReader();
    assertEquals(Reader.ProcessStatus.DONE, intReader.process(buffer));
    assertEquals(value1, intReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(Integer.BYTES, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());

    intReader.reset();

    assertEquals(Reader.ProcessStatus.DONE, intReader.process(buffer));
    assertEquals(value2, intReader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}