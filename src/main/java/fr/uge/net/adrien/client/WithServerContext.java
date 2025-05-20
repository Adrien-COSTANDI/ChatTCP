package fr.uge.net.adrien.client;

import fr.uge.net.adrien.packets.ClientPublicMessage;
import fr.uge.net.adrien.packets.ConnectAuth;
import fr.uge.net.adrien.packets.ConnectNoAuth;
import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.packets.DmConnect;
import fr.uge.net.adrien.packets.DmFileContent;
import fr.uge.net.adrien.packets.DmFileHeader;
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
      case ConnectNoAuth _, ConnectAuth _, ClientPublicMessage _, DmConnect _, DmText _,
           DmFileHeader _, DmFileContent _ -> {
      }
      case ConnectServerResponse connectServerResponse -> {
        switch (connectServerResponse.code()) {
          case OK -> System.out.println("connected");
          case PSEUDO_ALREADY_TAKEN -> {
            System.out.println("pseudo already taken");
            close();
            client.shutdown();
          }
          case INVALID_PSEUDO_OR_PASSWORD -> {
            System.out.println("invalid pseudo or password");
            close();
            client.shutdown();
          }
        }
      }
      case ServerForwardPublicMessage serverForwardPublicMessage -> System.out.println(
          "[" + serverForwardPublicMessage.pseudo() + "] " + serverForwardPublicMessage.contenu());
      case DmRequest dmRequest -> {
        if (client.isAlreadyFriendWith(dmRequest.pseudo())) {
          send(new DmResponse(dmRequest.pseudo(), DmResponse.Response.NO));
          return;
        }
        var nonce = ThreadLocalRandom.current().nextLong();
        client.addPendingDmRequest(dmRequest.pseudo());
        client.setNonceForFriend(dmRequest.pseudo(), nonce);
        client.display(
            dmRequest.pseudo() + " wants to connect. Use \"/accept " + dmRequest.pseudo() +
            "\" to accept or \"/deny \"" + dmRequest.pseudo() + "\" to refuse.");
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
        client.makeANewFriend(dmResponse.pseudo(), dmResponse.address().orElseThrow().address());
        client.confirmFriendship(dmResponse.address().orElseThrow().address(), dmResponse.pseudo());
        client.setNonceForFriend(dmResponse.pseudo(), dmResponse.nonce().orElseThrow());
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
