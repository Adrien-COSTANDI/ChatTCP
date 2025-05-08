package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpCodeReaderTest {

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);

    var reader = new OpCodeReader();
    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simple() {
    var buffer = ByteBuffer.allocate(1024);
    var value = Packet.Opcode.CONNECT_SERVER_RESPONSE;
    buffer.put(value.value());

    var reader = new OpCodeReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put(Packet.Opcode.CONNECT_SERVER_RESPONSE.value());
    buffer.limit(0); // not the size of a byte

    var reader = new OpCodeReader();
    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    buffer = ByteBuffer.allocate(1024);
    buffer.put(Packet.Opcode.CONNECT_SERVER_RESPONSE.value());
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(Packet.Opcode.CONNECT_SERVER_RESPONSE, reader.get());
  }


  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put(Packet.Opcode.CONNECT_SERVER_RESPONSE.value());

    var reader = new OpCodeReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new OpCodeReader();
    assertThrows(IllegalStateException.class, () -> {
      var res = reader.get();
    });
  }


  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = Packet.Opcode.CONNECT_SERVER_RESPONSE;
    var value2 = Packet.Opcode.CONNECT_AUTH;
    buffer.put(value1.value());
    buffer.put(value2.value());

    var reader = new OpCodeReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value1, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(Byte.BYTES, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());

    reader.reset();

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value2, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}