# ChatVaBien

This project aims to implement a "home-made" chat protocol.\
Find the RFC (redacted in French) in PDF format [here](./RFC%20ChatVaBien.pdf).\
It will be implemented using Java 24 for both the client and the server.

## Architecture of the server
The global architecture of the server will have one main blocking thread accepting connections from clients and a pool of non-blocking threads that will handle every client with a [Selector](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/channels/Selector.html) each

The main loop of those threads will use the [select](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/channels/Selector.html#select(java.util.function.Consumer)) method to check every client that has been registered into it so that it can process the data.

## Architecture of the client
The architecture of the client will have two threads :
- one for reading user inputs from the console
- one for managing the network part

The network thread will also be non-blocking with a [Selector](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/channels/Selector.html) and the console thread will be blocking, reading from the standard input.\
The messages will be processed from the console thread to the network thread using a custom thread-safe class.

## Client commands
A user will write messages and press `return` to send the message.\
If a message starts with `/` *(slash)*, it will be interpreted like a command.

Here is the list of commands a user can use :
- `/connect user` (alias `/c user`) send a request to `user` to start a private conversation
- `/accept user` (alias `/a user`)accept the request from `user` that asked for a private conversation
- `/deny user` (alias `/d user`) denies the request from `user` that asked for a private conversation
- `/requests (sent/received)` shows every pending request for private conversations the user has sent or received
- `/help` (alias `/h`) shows help for every command

