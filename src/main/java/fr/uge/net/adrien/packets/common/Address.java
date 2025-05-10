package fr.uge.net.adrien.packets.common;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Objects;

public record Address(InetSocketAddress address) {

  public static final int MAX_BYTES = 21;
      // 1 for the version + 16 for the IPV6 address + 4 for the port
  public static final String SEPARATOR = ":";

  public Address(InetSocketAddress address) {
    this.address = Objects.requireNonNull(address);
  }

  /**
   * Determine if InetSocketAdress is IPV4 or IPV6.
   *
   * @return IPV4 or IPV6
   */
  private IpVersion getIpVersion() {
    return switch (address.getAddress()) {
      case Inet4Address __ -> IpVersion.IPV4;
      case Inet6Address __ -> IpVersion.IPV6;
      default -> throw new IllegalStateException("Unexpected value: " + address.getAddress());
    };
  }

  /**
   * Converts the current address and port information into a {@code ByteBuffer}.
   * The buffer consists of a single byte representing the IP version (0 for IPv4, 1 for IPv6),
   * followed by the IP address bytes, and an integer representing the port.
   *
   * @return a {@code ByteBuffer} containing the serialized representation of the address and port
   */
  public ByteBuffer toByteBuffer() {
    // estimate the size of the buffer
    var addressBytesSize = address.getAddress().getAddress().length;
    var buffer = ByteBuffer.allocate(Byte.BYTES + addressBytesSize + Integer.BYTES);

    switch (getIpVersion()) {
      case IPV4 -> buffer.put((byte) 0);
      case IPV6 -> buffer.put((byte) 1);
      default -> throw new IllegalStateException("Unexpected value: " + getIpVersion());
    }

    // put the address and the port
    buffer.put(address.getAddress().getAddress());
    buffer.putInt(address.getPort());

    return buffer;
  }

  @Override
  public String toString() {
    return address.getAddress().toString().replace("/", "") + SEPARATOR + address.getPort();
  }

  enum IpVersion {
    IPV4, IPV6
  }
}
