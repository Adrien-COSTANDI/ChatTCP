package fr.uge.net.adrien.client;

import fr.uge.net.adrien.packets.ClientPublicMessage;
import fr.uge.net.adrien.packets.ConnectAuth;
import fr.uge.net.adrien.packets.ConnectNoAuth;
import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.packets.DmConnect;
import fr.uge.net.adrien.packets.DmRequest;
import fr.uge.net.adrien.packets.DmResponse;
import fr.uge.net.adrien.packets.DmText;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.packets.ServerForwardPublicMessage;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;


class FriendContext extends AbstractContext implements ClientContext {

  private final Client client;
  private SocketAddress friendAddress;
  private static final Logger logger = Logger.getLogger(FriendContext.class.getName());

  public FriendContext(SelectionKey key, Client client) {
    super(key);
    this.client = client;
    try {
      this.friendAddress = sc.getRemoteAddress();
    } catch (IOException e) {
      logger.info("failed to get friend address");
    }
  }

  public SocketAddress getFriendAddress() {
    return friendAddress;
  }

  protected void processReceivedPacket(Packet packet) {
    logger.info("received " + packet);
    switch (packet) {
      case ConnectNoAuth _, ConnectAuth _, ClientPublicMessage _, ConnectServerResponse _,
           ServerForwardPublicMessage _, DmRequest _, DmResponse _ -> {
      }
      case DmConnect dmConnect -> {
        if (dmConnect.nonce() != client.getNonceForFriend(dmConnect.pseudo())) {
          logger.info("received invalid nonce from " + dmConnect.pseudo());
          close();
          return;
        }
        try {
          client.confirmFriendship(sc.getRemoteAddress(), dmConnect.pseudo());
          client.sendToFriend(dmConnect.pseudo(), new DmText("hi " + dmConnect.pseudo() + " !"));
        } catch (IOException e) {
          client.display("failed to add friend " + dmConnect.pseudo());
          close();
          return;
        }
      }
      case DmText dmText ->
          client.display("[" + client.getFriend(friendAddress) + "] " + dmText.contenu());
    }
  }

  @Override
  public void doConnect() throws IOException {
    if (!sc.finishConnect()) {
      return;
    }
    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    friendAddress = sc.getRemoteAddress();
    client.display("I'm connected to " + friendAddress);
    // add friend here, but I don't have the name
    client.addAlmostFriend(friendAddress, this);
    send(new DmConnect(client.pseudo(), client.getNonceForFriend(client.getFriend(friendAddress))));
  }
}
