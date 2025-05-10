package fr.uge.net.adrien.readers;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InetAddressReaderTest {

  private ByteBuffer getIpv4() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) 0).put((byte) 1).put((byte) 2).put((byte) 3).put((byte) 4);
    return buffer;
  }

  private ByteBuffer getIpv6() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) 1)
        .put((byte) 0)
        .put((byte) 1)
        .put((byte) 0)
        .put((byte) 2)
        .put((byte) 0)
        .put((byte) 3)
        .put((byte) 0)
        .put((byte) 4)
        .put((byte) 0)
        .put((byte) 5)
        .put((byte) 0)
        .put((byte) 6)
        .put((byte) 0)
        .put((byte) 7)
        .put((byte) 0)
        .put((byte) 8);
    return buffer;
  }

  @Test
  public void simpleIPV4() {
    var buffer = getIpv4();
    InetAddressReader sr = new InetAddressReader();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));

    assertEquals("1.2.3.4", sr.get().getHostAddress());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void broadcast() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) 0).put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
    InetAddressReader sr = new InetAddressReader();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));

    assertEquals("255.255.255.255", sr.get().getHostAddress());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void errorIPVersion() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) -1).put((byte) 1).put((byte) 2).put((byte) 3).put((byte) 4);
    InetAddressReader sr = new InetAddressReader();
    assertEquals(Reader.ProcessStatus.ERROR, sr.process(buffer));
  }

  @Test
  public void errorMissingBytes() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put((byte) 0).put((byte) 1).put((byte) 2).put((byte) 3);
    InetAddressReader sr = new InetAddressReader();
    assertEquals(Reader.ProcessStatus.REFILL, sr.process(buffer));
  }

  @Test
  public void simpleIPV6() {
    var buffer = getIpv6();
    InetAddressReader sr = new InetAddressReader();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));

    assertEquals("1:2:3:4:5:6:7:8", sr.get().getHostAddress());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void reset() {
    var ip1 = getIpv4();
    var ip2 = getIpv6();
    var buffer = ByteBuffer.allocate(1024);
    buffer.put(ip1.flip()).put(ip2.flip());

    InetAddressReader sr = new InetAddressReader();

    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));
    assertEquals("1.2.3.4", sr.get().getHostAddress());
    assertEquals(17, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
    sr.reset();
    assertEquals(Reader.ProcessStatus.DONE, sr.process(buffer));
    assertEquals("1:2:3:4:5:6:7:8", sr.get().getHostAddress());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void smallBuffer() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put(getIpv4().flip()).flip();

    var bufferSmall = ByteBuffer.allocate(2);
    var sr = new InetAddressReader();
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
    assertEquals("1.2.3.4", sr.get().getHostAddress());
  }

  @Test
  public void errorGet() {
    var sr = new InetAddressReader();
    assertThrows(IllegalStateException.class, () -> {
      var res = sr.get();
    });
  }
}