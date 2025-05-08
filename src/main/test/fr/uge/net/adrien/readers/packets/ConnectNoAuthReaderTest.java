package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.ConnectNoAuth;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectNoAuthReaderTest {

  private static void checkOpCode(ByteBuffer buffer) {
    buffer.flip();
    assertEquals(Packet.Opcode.CONNECT_NO_AUTH.value(), buffer.get());
    buffer.compact();
  }

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);
    var reader = new ConnectNoAuthReader();

    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // Verify buffer position and limit
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new ConnectNoAuth("Bob");
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    var reader = new ConnectNoAuthReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new ConnectNoAuth("Bob");
    buffer.put(value.toByteBuffer().flip());
    buffer.limit(0); // not the size of a byte

    var reader = new ConnectNoAuthReader();
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
    var value = new ConnectNoAuth("Bob");
    buffer.put(value.toByteBuffer().flip());
    checkOpCode(buffer);

    var reader = new ConnectNoAuthReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new ConnectNoAuthReader();
    assertThrows(IllegalStateException.class, () -> {
      var res = reader.get();
    });
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = new ConnectNoAuth("Bob");
    var value2 = new ConnectNoAuth("Albert");
    buffer.put(value1.toByteBuffer().flip());
    buffer.put(value2.toByteBuffer().flip());

    var reader = new ConnectNoAuthReader();
    checkOpCode(buffer);
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value1, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(9, buffer.position());
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