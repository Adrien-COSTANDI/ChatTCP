package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;
import fr.uge.net.adrien.packets.DmResponse;

public final class Deny implements Command {

  private final String[] args;

  public Deny(String... args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("Command Deny requires one argument");
    }
    this.args = args;
  }

  @Override
  public void execute(Client client) {
    if (!client.hasPendingRequest(args[0])) {
      client.display("No pending request for " + args[0]);
      return;
    }
    client.sendToServer(new DmResponse(args[0], DmResponse.Response.NO));
    client.pendingRequestDone(args[0]);
  }
}
