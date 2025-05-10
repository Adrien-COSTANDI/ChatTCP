package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;

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
    System.out.println("Connect " + args[0]);
  }
}
