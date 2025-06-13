package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Share implements Command {

  private final String pseudo;
  private final Path path;

  public Share(String... args) {
    if (args.length < 2) {
      throw new IllegalArgumentException("Command Share requires 2 arguments");
    }
    this.pseudo = args[0];
    this.path = Path.of(args[1]);
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Command Share does not support directories");
    }
  }

  @Override
  public void execute(Client client) {
    client.sendFileToFriend(pseudo, path);
  }
}
