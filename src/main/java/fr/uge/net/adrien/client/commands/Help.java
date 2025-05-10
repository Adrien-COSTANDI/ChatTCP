package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;

public final class Help implements Command {

  @Override
  public void execute(Client client) {
    System.out.println("Help command");
  }
}
