package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;
import fr.uge.net.adrien.packets.DmRequest;

public final class Connect implements Command {

  private final String[] args;

  public Connect(String... args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("Command Connect requires one argument");
    }
    this.args = args;
  }

  @Override
  public void execute(Client client) {
    client.sendToServer(new DmRequest(args[0]));
  }
}
