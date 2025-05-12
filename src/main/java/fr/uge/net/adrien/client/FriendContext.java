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
import java.nio.channels.SelectionKey;


class FriendContext extends AbstractContext implements ClientContext {

  private final Client client;
  private long nonce;
  private String friendPseudo;

  public FriendContext(SelectionKey key, Client client) {
    super(key);
    this.client = client;
  }


  protected void processReceivedPacket(Packet packet) {
    System.out.println("received " + packet);
    switch (packet) {
      case ConnectNoAuth _, ConnectAuth _, ClientPublicMessage _, ConnectServerResponse _,
           ServerForwardPublicMessage _, DmRequest _, DmResponse _ -> {
      }
      case DmConnect dmConnect -> {
        if (dmConnect.nonce() != nonce) {
          System.out.println("received invalid nonce from " + dmConnect.pseudo());
          silentlyClose();
          return;
        }
        try {
          client.confirmFriendship(sc.getRemoteAddress(), dmConnect.pseudo());
          friendPseudo = dmConnect.pseudo();
          client.sendToFriend(dmConnect.pseudo(), new DmText("hi " + dmConnect.pseudo() + " !"));
        } catch (IOException e) {
          System.out.println("failed to add friend " + dmConnect.pseudo());
          silentlyClose();
          return;
        }
      }
      case DmText dmText -> {
        try {
          System.out.println(
              "[" + client.getFriend(sc.getRemoteAddress()) + "] " + dmText.contenu());
        } catch (IOException e) {
          System.out.println("Impossible d'afficher la provenance du message");
        }
      }
    }
  }

  @Override
  public void doConnect() throws IOException {
    if (!sc.finishConnect()) {
      return;
    }
    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    System.out.println("I'm connected to " + sc.getRemoteAddress());
    // add friend here, but I don't have the name
    client.addAlmostFriend(sc.getRemoteAddress(), this);
    send(new DmConnect(client.pseudo(), nonce));
  }
}
