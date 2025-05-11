package fr.uge.net.adrien.client;

import fr.uge.net.adrien.packets.Packet;
import java.util.HashMap;

class FriendManager {

  private final HashMap<String, FriendContext> friends = new HashMap<>();

  public void addFriend(String friend, FriendContext context) {
    friends.put(friend, context);
  }

  public void removeFriend(String friend) {
    friends.remove(friend);
  }

  public void sendTo(String friend, Packet packet) {
    var context = friends.get(friend);
    if (context == null) {
      throw new IllegalArgumentException("Friend not found: " + friend);
    }
    context.send(packet);
  }
}
