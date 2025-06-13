package fr.uge.net.adrien.client;

import fr.uge.net.adrien.packets.ClientPublicMessage;
import fr.uge.net.adrien.packets.ConnectAuth;
import fr.uge.net.adrien.packets.ConnectNoAuth;
import fr.uge.net.adrien.packets.ConnectServerResponse;
import fr.uge.net.adrien.packets.DmConnect;
import fr.uge.net.adrien.packets.DmFileContent;
import fr.uge.net.adrien.packets.DmFileHeader;
import fr.uge.net.adrien.packets.DmRequest;
import fr.uge.net.adrien.packets.DmResponse;
import fr.uge.net.adrien.packets.DmText;
import fr.uge.net.adrien.packets.Packet;
import fr.uge.net.adrien.packets.ServerForwardPublicMessage;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;


class FriendContext extends AbstractContext implements ClientContext {

  private final Client client;
  private SocketAddress friendAddress;
  private static final Logger logger = Logger.getLogger(FriendContext.class.getName());

  private OutputStream receivedFile;
  private int remainingFileSize = -1;
  private String expectedFileName = null;

  private SeekableByteChannel sentFile;

  public FriendContext(SelectionKey key, Client client) throws IOException {
    super(key);
    this.client = client;
    receivedFile = Files.newOutputStream(client.getPathToFolder());
    try {
      this.friendAddress = sc.getRemoteAddress();
    } catch (IOException e) {
      logger.info("failed to get friend address");
    }
  }

  public SocketAddress getFriendAddress() {
    return friendAddress;
  }

  protected void processReceivedPacket(Packet packet) {
    logger.info("received " + packet);
    switch (packet) {
      case ConnectNoAuth _, ConnectAuth _, ClientPublicMessage _, ConnectServerResponse _,
           ServerForwardPublicMessage _, DmRequest _, DmResponse _ -> {
      }
      case DmConnect dmConnect -> {
        if (dmConnect.nonce() != client.getNonceForFriend(dmConnect.pseudo())) {
          logger.info("received invalid nonce from " + dmConnect.pseudo());
          close();
          return;
        }
        try {
          client.confirmFriendship(sc.getRemoteAddress(), dmConnect.pseudo());
          client.sendToFriend(dmConnect.pseudo(), new DmText("hi " + dmConnect.pseudo() + " !"));
        } catch (IOException e) {
          client.display("failed to add friend " + dmConnect.pseudo());
          close();
          return;
        }
      }
      case DmText dmText -> client.display(
          "[" + client.getFriend(friendAddress) + "] says \"" + dmText.contenu() + "\" to you.");
      case DmFileHeader dmFileHeader -> {
        if (remainingFileSize != -1 || expectedFileName != null) {
          logger.warning("received unexpected file header, resetting");
          try {
            receivedFile.close();
            Files.deleteIfExists(client.getPathToFolder().resolve(expectedFileName));
          } catch (IOException e) {
            logger.warning("failed to delete file " + expectedFileName + ":\n" + e);
          }
        }
        remainingFileSize = dmFileHeader.size();
        expectedFileName = dmFileHeader.fileName();
        try {
          receivedFile = Files.newOutputStream(client.getPathToFolder().resolve(expectedFileName));
        } catch (IOException e) {
          logger.warning("failed to create file " + expectedFileName + ":\n" + e);
          silentlyClose(receivedFile);
        }
      }
      case DmFileContent dmFileContent -> {
        /*
         dmFileContent.contenu() écrire dans le flux vers le fichier
         if fichier.finished throw ISE
         else write into fichier and close if necessary

         outputStream.write(dmFileContent.contenu());
        */

        try {
          receivedFile.write(dmFileContent.contenu());
        } catch (IOException e) {
          logger.warning("failed to write to file " + expectedFileName + ":\n" + e);
          silentlyClose(receivedFile);
          remainingFileSize = -1;
          expectedFileName = null;
          return;
        }
        remainingFileSize -= dmFileContent.contenu().length;
        if (remainingFileSize < 0) {
          logger.warning("received more bytes than expected");
        }

        if (remainingFileSize <= 0) {
          silentlyClose(receivedFile);
          client.display("File " + expectedFileName + " received");
          remainingFileSize = -1;
          expectedFileName = null;
        }
      }
    }
  }


  public void setFileToSend(Path file) {
    try {
      sentFile = Files.newByteChannel(file);
    } catch (IOException e) {
      silentlyClose(sentFile);
    }
  }

  private static void silentlyClose(Closeable c) {
    try {
      c.close();
    } catch (IOException e) {
      // ignore
    }
  }

  @Override
  public void doConnect() throws IOException {
    if (!sc.finishConnect()) {
      return;
    }
    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    friendAddress = sc.getRemoteAddress();
    client.display("I'm connected to " + friendAddress);
    // add friend here, but I don't have the name
    client.addAlmostFriend(friendAddress, this);
    send(new DmConnect(client.pseudo(), client.getNonceForFriend(client.getFriend(friendAddress))));
  }

  @Override
  protected void processOut() {
    while (!queue.isEmpty() && bufferOut.remaining() >= queue.peek().length()) {
      var packetBuffer = queue.poll().toByteBuffer().flip();
      bufferOut.put(packetBuffer);
    }
    var remainingBufferOut = bufferOut.remaining();
    try {
      var fileBuffer = ByteBuffer.allocate(Math.min(remainingBufferOut, 8192) - 1 - 2);
      var bytesRead = sentFile.read(fileBuffer);
      if (bytesRead == -1) {
        sentFile.close();
      }
    } catch (IOException e) {
      try {
        logger.warning("failed to send file " + expectedFileName + ":\n" + e);
        sentFile.close();
      } catch (IOException ex) {
        // ignore
      }
    }
  }
}
