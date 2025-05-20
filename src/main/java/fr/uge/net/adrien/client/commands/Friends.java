package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;
import java.util.Set;
import java.util.stream.Collectors;

public final class Friends implements Command {

  @Override
  public void execute(Client client) {
    Set<String> friends = client.friends();
    if (friends.isEmpty()) {
      client.display("No friends");
      return;
    }
    var str = friends.stream().collect(Collectors.joining(", ", "Friends: ", "\n;)"));
    client.display(str);
  }
}
