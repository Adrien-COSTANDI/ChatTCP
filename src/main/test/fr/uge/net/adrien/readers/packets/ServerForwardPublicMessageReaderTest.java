package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.packets.ServerForwardPublicMessage;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerForwardPublicMessageReaderTest {

  private static void checkOpCode(ByteBuffer buffer) {
    buffer.flip();
    assertEquals(Packet.Opcode.SERVER_FORWARD_PUBLIC_MESSAGE.value(), buffer.get());
    buffer.compact();
  }

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);
    var reader = new ServerForwardPublicMessageReader();

    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // Verify buffer position and limit
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new ServerForwardPublicMessage("Contenu du message", "Alice");
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    var reader = new ServerForwardPublicMessageReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    var value = new ServerForwardPublicMessage("Contenu du message", "Alice");
    buffer.put(value.toByteBuffer().flip());
    buffer.limit(0); // not the size of a byte

    var reader = new ServerForwardPublicMessageReader();
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
    var value = new ServerForwardPublicMessage("Contenu du message", "Alice");
    buffer.put(value.toByteBuffer().flip());
    checkOpCode(buffer);

    var reader = new ServerForwardPublicMessageReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new ServerForwardPublicMessageReader();
    assertThrows(IllegalStateException.class, reader::get);
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = new ServerForwardPublicMessage("Contenu du message", "Alice");
    var value2 = new ServerForwardPublicMessage("Contenu d'un autre message", "Bob");
    buffer.put(value1.toByteBuffer().flip());
    buffer.put(value2.toByteBuffer().flip());

    var reader = new ServerForwardPublicMessageReader();
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