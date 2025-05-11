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
           ServerForwardPublicMessage _ -> {
      }
      case DmRequest dmRequest -> {
        client.sendToServer(new DmResponse(dmRequest.pseudo(), DmResponse.Response.NO));
      }
      case DmResponse dmResponse -> {

      }
      case DmConnect dmConnect -> {
        if (dmConnect.nonce() != nonce) {
          System.out.println("received invalid nonce from " + dmConnect.pseudo());
          silentlyClose();
          return;
        }
        friendPseudo = dmConnect.pseudo();
        client.sendToServer(new DmText("hi " + dmConnect.pseudo() + " !"));
      }
      case DmText dmText -> {
        System.out.println("[" + friendPseudo + "] " + dmText.contenu());
      }
    }
  }

  @Override
  public void doConnect() throws IOException {
    if (!sc.finishConnect()) {
      return;
    }
    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    System.out.println("my address : " + sc.getLocalAddress());

    send(new DmConnect(client.pseudo(), nonce));
  }
}
