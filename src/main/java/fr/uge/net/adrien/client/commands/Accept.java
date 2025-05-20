package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;
import fr.uge.net.adrien.packets.DmResponse;

public final class Accept implements Command {

  private final String[] args;

  public Accept(String... args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("Command Accept requires one argument");
    }
    this.args = args;
  }

  @Override
  public void execute(Client client) {
    if (!client.hasPendingRequest(args[0])) {
      client.display("No pending request for " + args[0]);
      return;
    }
    client.sendToServer(new DmResponse(args[0],
                                       DmResponse.Response.YES,
                                       client.getNonceForFriend(args[0]),
                                       client.address()));
    client.pendingRequestDone(args[0]);
  }
}
