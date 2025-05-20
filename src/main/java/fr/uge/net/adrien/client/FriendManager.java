package fr.uge.net.adrien.client;

import fr.uge.net.adrien.packets.Packet;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Set;

class FriendManager {

  private final HashMap<String, FriendContext> friends = new HashMap<>();
  private final HashMap<SocketAddress, FriendContext> almostFriends = new HashMap<>();
  private final HashMap<SocketAddress, String> friendsByAddr = new HashMap<>();
  private final HashMap<String, Long> friendNonce = new HashMap<>();

  public void addFriend(String friend, FriendContext context) {
    friends.putIfAbsent(friend, context);
    try {
      friendsByAddr.put(context.sc.getRemoteAddress(), friend);
    } catch (IOException e) {
      throw new AssertionError("Couldn't get remote address of " + friend);
    }
  }

  public void addAlmostFriend(SocketAddress address, FriendContext context) {
    almostFriends.put(address, context);
  }

  public String getFriend(SocketAddress address) {
    return friendsByAddr.get(address);
  }

  public void removeFriend(String friend) {
    var context = friends.remove(friend);
    if (context == null) {
      return;
    }
    friendsByAddr.remove(context.getFriendAddress());
  }

  public void sendTo(String friend, Packet packet) {
    var context = friends.get(friend);
    if (context == null) {
      throw new IllegalArgumentException("Friend not found: " + friend);
    }
    context.send(packet);
  }

  public void confirmFriendShip(SocketAddress address, String pseudo) {
    var trueFriend = almostFriends.remove(address);
    if (trueFriend == null) {
      return;
    }
    addFriend(pseudo, trueFriend);
  }

  public void setNonceForFriend(String pseudo, long nonce) {
    friendNonce.put(pseudo, nonce);
  }

  public long getNonceForFriend(String pseudo) {
    return friendNonce.getOrDefault(pseudo, 0L);
  }

  public Set<String> getAllFriends() {
    return friends.keySet();
  }
}
