package fr.uge.net.adrien;

import java.io.IOException;

public interface Context {

  void doWrite() throws IOException;

  void doRead() throws IOException;

  void close();
}
