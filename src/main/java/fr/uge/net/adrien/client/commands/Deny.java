package fr.uge.net.adrien.client.commands;

public final class Deny implements Command {

  private final String[] args;

  public Deny(String... args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("Command Deny requires one argument");
    }
    this.args = args;
  }

  @Override
  public void execute() {
    System.out.println("Deny " + args[0]);
  }
}
