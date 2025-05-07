package fr.uge.net.adrien.client.commands;

public final class Requests implements Command {

  private final String[] args;

  public Requests(String... args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("Command Requests requires one argument");
    }
    this.args = args;
  }

  @Override
  public void execute() {
    System.out.println("Requests " + args[0]);
  }
}
