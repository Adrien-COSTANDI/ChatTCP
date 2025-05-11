package fr.uge.net.adrien.client.commands;

import fr.uge.net.adrien.client.Client;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a command that can be executed by the client. This sealed interface
 * defines the structure for various command implementations such as Accept,
 * Connect, Deny, Help, and Requests.
 * <p>
 * Each command is expected to provide its own implementation of the {@code execute()}
 * method, defining the behavior of the command when it is executed.
 * <p>
 * Commands are represented as strings starting with a specific prefix, defined
 * by the {@code COMMAND_PREFIX} constant. The prefix helps to distinguish
 * commands from other inputs.
 */
public sealed interface Command permits Accept, Connect, Deny, Help, Requests {

  char COMMAND_PREFIX = '/';

  /**
   * Executes the command with the given {@link Client}.
   *
   * @param client the client to operate the command with
   */
  void execute(Client client);

  /**
   * Parses a given input line and returns the corresponding command instance.
   * The input line must specify a command starting with the specified prefix
   * and optionally followed by arguments separated by whitespace.
   * <p>
   * Supported commands:
   * - "connect" or "c": Creates a {@link Connect} command instance.
   * - "accept" or "a": Creates an {@link Accept} command instance.
   * - "deny" or "d": Creates a {@link Deny} command instance.
   * - "requests": Creates a {@link Requests} command instance.
   * - "help" or "h": Creates a {@link Help} command instance.
   * <p>
   * If the command is not recognized or the input is invalid (e.g., empty line,
   * missing prefix, missing required arguments), an exception is thrown.
   *
   * @param line the input line representing the command, not null
   * @return the corresponding {@link Command} instance
   * @throws NullPointerException if the input line is null
   * @throws IllegalArgumentException if the input line is blank, does not start
   *         with the expected prefix or specifies an unknown command
   */
  static Command parse(String line) {
    Objects.requireNonNull(line);
    if (line.isBlank()) {
      throw new IllegalArgumentException("Empty line is not a valid command");
    }
    line = line.trim();

    var array = line.split("[ \t]+", 2);
    var command = array[0].substring(1);
    var args = Arrays.copyOfRange(array, 1, array.length);

    return switch (command) {
      case "connect", "c" -> new Connect(args);
      case "accept", "a" -> new Accept(args);
      case "deny", "d" -> new Deny(args);
      case "requests" -> new Requests(args);
      case "help", "h" -> new Help();
      default -> throw new IllegalArgumentException("Unknown command: " + command);
    };
  }

}
