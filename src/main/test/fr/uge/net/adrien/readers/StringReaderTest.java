package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringReaderTest {

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);

    var sr = new StringReader();
    assertEquals(Reader.ProcessStatus.REFILL, sr.process(buffer));

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }


  @Test
  public void simple() {
    var string = "\u20ACa\u20AC";
    var buffer = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode(string);
    buffer.putInt(bytes.remaining()).put(bytes);
    StringReader sr = new StringReader();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));
    assertEquals(string, sr.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void reset() {
    var string = "\u20ACa\u20AC";
    var string2 = "\u20ACa\u20ACabcd";
    var buffer = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode(string);
    var bytes2 = StandardCharsets.UTF_8.encode(string2);
    buffer.putInt(bytes.remaining()).put(bytes).putInt(bytes2.remaining()).put(bytes2);
    StringReader sr = new StringReader();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));
    assertEquals(string, sr.get());
    assertEquals(15, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    sr.reset();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));
    assertEquals(string2, sr.get());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void smallBuffer() {
    var string = "\u20ACa\u20AC";
    var buffer = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode(string);
    buffer.putInt(bytes.remaining()).put(bytes).flip();
    var bufferSmall = ByteBuffer.allocate(2);
    var sr = new StringReader();
    while (buffer.hasRemaining()) {
      while (buffer.hasRemaining() && bufferSmall.hasRemaining()) {
        bufferSmall.put(buffer.get());
      }
      if (buffer.hasRemaining()) {
        assertEquals(Reader.ProcessStatus.REFILL, sr.process(bufferSmall));
      } else {
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bufferSmall));
      }
    }
    assertEquals(string, sr.get());
  }

  @Test
  public void errorGet() {
    var sr = new StringReader();
    assertThrows(IllegalStateException.class, () -> {
      var res = sr.get();
    });
  }

  @Test
  public void errorNeg() {
    var sr = new StringReader();
    var buffer = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode("aaaaa");
    buffer.putInt(-1).put(bytes);
    assertEquals(Reader.ProcessStatus.ERROR, sr.process(buffer));
  }

  @Test
  public void errorTooBig() {
    var sr = new StringReader();
    var buffer = ByteBuffer.allocate(1024);
    var bytes = StandardCharsets.UTF_8.encode("aaaaa");
    buffer.putInt(1025).put(bytes);
    assertEquals(Reader.ProcessStatus.ERROR, sr.process(buffer));
  }
}