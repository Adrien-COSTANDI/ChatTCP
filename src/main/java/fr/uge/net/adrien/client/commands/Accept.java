package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;

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
    System.out.println("Accept " + args[0]);
  }
}
