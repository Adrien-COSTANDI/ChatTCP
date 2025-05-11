package fr.uge.net.adrien.client;

import fr.uge.net.adrien.Context;
import java.io.IOException;

public interface ClientContext extends Context {

  void doConnect() throws IOException;
}
