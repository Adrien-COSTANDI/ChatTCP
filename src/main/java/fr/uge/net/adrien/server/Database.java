package fr.uge.net.adrien.server;

import java.util.Map;

public class Database {

  private static final Database instance = new Database();
  private final Map<String, String> database = Map.of("Alice", "1234", "Bob", "1234");

  private Database() { }

  public static Database getInstance() {
    return instance;
  }

  public boolean passwordMatch(String username, String password) {
    String passwordDb = database.get(username);
    return passwordDb != null && passwordDb.equals(password);
  }

  public boolean usernameExists(String username) {
    return database.containsKey(username);
  }
}
