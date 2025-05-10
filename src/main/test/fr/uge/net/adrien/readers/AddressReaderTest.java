package fr.uge.net.adrien.readers;

import fr.uge.net.adrien.packets.common.Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressReaderTest {

  @Test
  public void simpleIPV4() {
    var buffer = ByteBuffer.allocate(21);
    var value = new Address(new InetSocketAddress("1.2.3.4", 8888));
    buffer.put(value.toByteBuffer().flip());

    var addressReader = new AddressReader();
    assertEquals(Reader.ProcessStatus.DONE, addressReader.process(buffer));
    var adrs = addressReader.get();
    assertEquals(adrs.address(), value.address());

    // check that we have put back in writing mode
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  public void simpleIPV6() {
    var buffer = ByteBuffer.allocate(40);
    var address = new Address(new InetSocketAddress("aabb:bbbb:cccc:9826:eeee:ffff::0000", 8888));
    buffer.put(address.toByteBuffer().flip());

    var addressReader = new AddressReader();
    assertEquals(Reader.ProcessStatus.DONE, addressReader.process(buffer));
    var adrs = addressReader.get();
    assertEquals(adrs.address(), address.address());

    // check that we have put back in writing mode
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }


  @Test
  public void illegalPort() {
    var buffer = ByteBuffer.allocate(21);
    var value = new Address(new InetSocketAddress("1.2.3.4", 80));
    buffer.put(value.toByteBuffer().flip());

    var addressReader = new AddressReader();
    assertEquals(Reader.ProcessStatus.ERROR, addressReader.process(buffer));

    // check that we have put back in writing mode
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }

  @Test
  void processRefill() {
    var buffer = ByteBuffer.allocate(21);

    var addressReader = new AddressReader();
    assertEquals(Reader.ProcessStatus.REFILL, addressReader.process(buffer));

    buffer = ByteBuffer.allocate(21);
    var address = new Address(new InetSocketAddress("1.2.3.4", 8888));
    buffer.put(address.toByteBuffer().flip());
    assertEquals(Reader.ProcessStatus.DONE, addressReader.process(buffer));

    var buffer2 = ByteBuffer.allocate(21);
    var addressReader2 = new AddressReader();
    assertEquals(Reader.ProcessStatus.REFILL, addressReader2.process(buffer2));
    buffer2 = ByteBuffer.allocate(40);
    var address2 = new Address(new InetSocketAddress("aabb:bbbb:cccc:9826:eeee:ffff::0000", 8888));
    buffer2.put(address2.toByteBuffer().flip());
    assertEquals(Reader.ProcessStatus.DONE, addressReader2.process(buffer2));

  }

  @Test
  void processTwice() {
    var buffer = ByteBuffer.allocate(21);
    var address = new Address(new InetSocketAddress("1.2.3.4", 8888));
    buffer.put(address.toByteBuffer().flip());

    var addressReader = new AddressReader();
    assertEquals(Reader.ProcessStatus.DONE, addressReader.process(buffer));
    assertThrows(IllegalStateException.class, () -> addressReader.process(buffer));
  }

  @Test
  public void errorGet() {
    var addressReader = new AddressReader();
    assertThrows(IllegalStateException.class, addressReader::get);
  }

  @Test
  void reset() {
    var buffer = ByteBuffer.allocate(21);
    var address = new Address(new InetSocketAddress("1.2.3.4", 8888));
    buffer.put(address.toByteBuffer().flip());

    var addressReader = new AddressReader();
    assertEquals(Reader.ProcessStatus.DONE, addressReader.process(buffer));

    addressReader.reset();

    var buffer2 = ByteBuffer.allocate(21);
    var address2 = new Address(new InetSocketAddress("1.2.3.4", 8888));
    buffer2.put(address2.toByteBuffer().flip());

    assertEquals(Reader.ProcessStatus.DONE, addressReader.process(buffer2));
    var adrs = addressReader.get();
    assertEquals(address.address(), adrs.address());
    assertEquals(0, buffer.position());
    assertEquals(buffer.capacity(), buffer.limit());
  }
}