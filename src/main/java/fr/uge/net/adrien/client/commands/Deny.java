package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;

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
    System.out.println("Deny " + args[0]);
  }
}
