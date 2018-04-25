# Client-Server-Messaging-Program
A client/server implementation of a simple messaging app.
Classes:
  client/ClientSide
    Client side of the program that implemements my Chatterbox protocol messages as well as Java's built in Socket
    class.
  common/ChatMessage
    Class for representing a message that implements serializable to write the objects to the the servers inputstream
  common/ChatterboxProtocol
    Interface for holding the necessary protocols of my program
  server/ServerSide
    Server side of the program that listens for connections from clients and handles commands typed by the client
    
  
