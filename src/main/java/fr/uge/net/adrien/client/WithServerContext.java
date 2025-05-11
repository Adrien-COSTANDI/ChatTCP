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
import java.util.concurrent.ThreadLocalRandom;

class WithServerContext extends AbstractContext implements ClientContext {

  private final Client client;

  public WithServerContext(SelectionKey key, Client client) {
    super(key);
    this.client = client;
  }

  @Override
  protected void processReceivedPacket(Packet packet) {
    System.out.println("received " + packet);
    switch (packet) {
      case ConnectNoAuth _, ConnectAuth _, ClientPublicMessage _ -> {
      }
      case ConnectServerResponse connectServerResponse -> {
        switch (connectServerResponse.code()) {
          case OK -> System.out.println("connected");
          case PSEUDO_ALREADY_TAKEN -> {
            System.out.println("pseudo already taken");
            silentlyClose();
            client.shutdown();
          }
          case INVALID_PSEUDO_OR_PASSWORD -> {
            System.out.println("invalid pseudo or password");
            silentlyClose();
            client.shutdown();
          }
        }
      }
      case ServerForwardPublicMessage serverForwardPublicMessage -> System.out.println(
          "[" + serverForwardPublicMessage.pseudo() + "] " + serverForwardPublicMessage.contenu());
      case DmRequest dmRequest -> { // TODO
        var nonce = ThreadLocalRandom.current().nextLong();
        client.sendToServer(new DmResponse(dmRequest.pseudo(),
                                           DmResponse.Response.YES,
                                           nonce,
                                           client.address()));
      }
      case DmResponse dmResponse -> {
        switch (dmResponse.ok()) {
          case YES -> {
            System.out.println("received yes from " + dmResponse.pseudo());
          }
          case NO -> {
            System.out.println("received no from " + dmResponse.pseudo());
          }
        }
        if (dmResponse.ok() == DmResponse.Response.NO) {
          System.out.println(dmResponse.pseudo() + " refused your request");
          return;
        }

        FriendContext friendContext = new FriendContext(key, client);
        // client.addFriend(dmResponse.pseudo(), friendContext); // TODO PAS ICI
        friendContext.send(new DmConnect(client.pseudo(), 123)); // TODO nonce
      }
      case DmConnect dmConnect -> {
        // client.addFriend(dmConnect.pseudo(), new FriendContext(key, client));
      }
      case DmText dmText -> {
      }
    }
  }

  @Override
  public void doConnect() throws IOException {
    if (!sc.finishConnect()) {
      return;
    }
    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    System.out.println("connected to server");
    // Possiblement envoyer un message d’identification ici
    var password = client.password();
    Packet connectPacket = password.isPresent() ? new ConnectAuth(client.pseudo(), password.get()) :
        new ConnectNoAuth(client.pseudo());
    send(connectPacket);
  }
}
