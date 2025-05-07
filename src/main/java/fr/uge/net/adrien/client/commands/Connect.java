package fr.uge.net.adrien.client.commands;

public final class Connect implements Command {

  private final String[] args;

  public Connect(String... args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("Command Connect requires one argument");
    }
    this.args = args;
  }

  @Override
  public void execute() {
    System.out.println("Connect " + args[0]);
  }
}
