package fr.uge.net.adrien.readers.packets;

import fr.uge.net.adrien.packets.ConnectNoAuth;
import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.readers.Reader;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PacketReaderTest {

  public static List<Packet> packets() {
    return List.of(new ConnectNoAuth("Bob"),
        // new ConnectAuth("Bob", "password1234"), TODO
        new ConnectServerResponse(ConnectServerResponse.StatusCode.OK));
  }

  @Test
  public void empty() {
    var buffer = ByteBuffer.allocate(1024);
    var reader = new PacketReader();

    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    // Verify buffer position and limit
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @ParameterizedTest
  @MethodSource("packets")
  public void simple(Packet packet) {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put(packet.toByteBuffer().flip());

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(packet, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @ParameterizedTest
  @MethodSource("packets")
  void processRefill(Packet packet) {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put(packet.toByteBuffer().flip());
    buffer.limit(0); // not the size of a byte

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer));

    buffer = ByteBuffer.allocate(1024);
    buffer.put(packet.toByteBuffer().flip());

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(packet, reader.get());
  }

  @ParameterizedTest
  @MethodSource("packets")
  void processTwice(Packet packet) {
    var buffer = ByteBuffer.allocate(1024);
    buffer.put(packet.toByteBuffer().flip());

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertThrows(IllegalStateException.class, () -> reader.process(buffer));
  }

  @Test
  public void errorGet() {
    var reader = new PacketReader();
    assertThrows(IllegalStateException.class, reader::get);
  }

  @ParameterizedTest
  @MethodSource("packets")
  void reset(Packet packet) {
    var buffer = ByteBuffer.allocate(1024);
    var value2 = new ConnectNoAuth("Albert");
    buffer.put(packet.toByteBuffer().flip());
    buffer.put(value2.toByteBuffer().flip());

    var reader = new PacketReader();
    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(packet, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(value2.length(), buffer.position()); // Albert
    assertEquals(buffer.capacity(), buffer.limit());

    reader.reset();

    assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
    assertEquals(value2, reader.get());

    // vérifie qu'on a remis en mode ecriture (compact)
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}