package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.DmResponse;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.packets.common.Address;
import fr.uge.net.adrien.readers.Reader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmResponseReaderTest {

  private final static DmResponse dmResponseYes = new DmResponse("pseudo",
                                                                 DmResponse.Response.YES,
                                                                 Optional.of(123L),
                                                                 Optional.of(new Address(new InetSocketAddress(
                                                                     "localhost",
                                                                     9999))));
  private final static DmResponse dmResponseNo =
      new DmResponse("pseudo", DmResponse.Response.NO, Optional.empty(), Optional.empty());
  private final static DmResponse dmResponseWrong =
      new DmResponse("pseudo", DmResponse.Response.YES, Optional.empty(), Optional.empty());

  private static void checkOpCode(ByteBuffer buffer) {
    buffer.flip();
    assertEquals(Packet.Opcode.DM_RESPONSE.value(), buffer.get());
    buffer.compact();
  }

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);
    var reader = new DmResponseReader();

    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // Verify buffer position and limit
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simpleYes() {
    var buffer = ByteBuffer.allocate(1024);
    var value = dmResponseYes;
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    var reader = new DmResponseReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simpleNo() {
    var buffer = ByteBuffer.allocate(1024);
    var value = dmResponseNo;
    buffer.put(value.toByteBuffer().flip());

    checkOpCode(buffer);

    var reader = new DmResponseReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }


  @Test
  public void simpleWrong() {
    var value = dmResponseWrong;
    assertThrows(IllegalStateException.class, () -> value.toByteBuffer().flip());
  }


  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(1024);
    var value = dmResponseYes;
    buffer.put(value.toByteBuffer().flip());
    buffer.limit(0); // not the size of a byte

    var reader = new DmResponseReader();
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
    var value = dmResponseYes;
    buffer.put(value.toByteBuffer().flip());
    checkOpCode(buffer);

    var reader = new DmResponseReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new DmResponseReader();
    assertThrows(IllegalStateException.class, reader::get);
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(1024);
    var value1 = dmResponseYes;
    var value2 = dmResponseNo;
    buffer.put(value1.toByteBuffer().flip());
    buffer.put(value2.toByteBuffer().flip());

    var reader = new DmResponseReader();
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