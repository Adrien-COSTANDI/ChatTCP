package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;
import fr.uge.net.adrien.packets.DmText;

public final class Tell implements Command {

  private final String message;
  private final String pseudo;

  public Tell(String pseudo, String message) {
    this.pseudo = pseudo;
    this.message = message;
  }

  @Override
  public void execute(Client client) {
    client.sendToFriend(pseudo, new DmText(message));
  }
}
