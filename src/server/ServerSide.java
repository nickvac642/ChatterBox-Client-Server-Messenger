package server;


import common.ChatMessage;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;


/**
 * This is the Server class that will listen for connections from clients and handle the commands a client mey enter
 * Author: Nick Vaccarello
 * email: nwv4110@rit.edu
 */
public class ServerSide implements common.ChatterboxProtocol {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> clients;
    // to display time for each message
    private SimpleDateFormat sdf;
    // the port number to listen for connection
    private int port;
    // to keep the server running unless specified
    private boolean continueToRun;
    // notification for specific outputs
    private String notif = " *** ";

    /**
     * Constructor for server, constructs the server with the port, time formatter, and an empty list of clients
     * @param port
     */

    public ServerSide(int port) {
        this.port = port;
        sdf = new SimpleDateFormat("h:mm a");
        clients = new ArrayList<ClientThread>();
    }

    /**
     * The start method starts the server and begins to look for connections between a client
     */
    public void start() {
        continueToRun = true;
        try {
            // the socket used by the server with the port
            ServerSocket serverSocket = new ServerSocket(port);

            // Loop to continuously look for connections
            while (continueToRun) {
                display("Waiting connection on port: " + port + ".");
                // accept connection if requested from client
                Socket socket = serverSocket.accept();
                display("ChatterboxClient connection received from "+socket.getInetAddress());
                // break if server stopped
                if (!continueToRun)
                    break;
                // Create thread for connected client
                ClientThread client = new ClientThread(socket);
                //add this client to arraylist
                clients.add(client);
                client.start();
            }
            // When the server has stop running, attempt to close the data streams
            try {
                serverSocket.close();
                for (int i = 0; i < clients.size(); ++i) {
                    ClientThread tc = clients.get(i);
                    try {
                        // close all data streams and socket
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                    }
                }
            } catch (Exception e) {
                display(">>"+FATAL_ERROR+SEPARATOR+" Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String msg = sdf.format(new Date()) +">>"+FATAL_ERROR+SEPARATOR+" Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    /**
     * Display function for the server's console
     * @param msg-the message being displayed
     */
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    /**
     * This method broadcasts the appropriate display to the client(s) depending on the command
     * @param message-message being broadcast
     * @param isPrivate-boolean for determining if the message is sent privately
     * @return
     */
    private synchronized boolean broadcast(String message, boolean isPrivate,String userName) {
        // add timestamp to the message
        String time = sdf.format(new Date());
        //splits the message into user, then message
        String[] w = message.split(" ", 2);
        // if private message, send message to mentioned userName only
        if (isPrivate) {
            String tocheck = w[0];
            message =userName +" (private message): " + w[1];
            String messageLf = time + " " + message + "\n";
            boolean found = false;
            //Loop in reverse to find the userName
            for (int y = clients.size(); --y >= 0; ) {
                ClientThread ct1 = clients.get(y);
                String check = ct1.getuserName();
                if (check.equals(tocheck)) {
                    //try to write to Client if it fails remove it from list since that client is no longer connected
                    if (!ct1.writeMsg(messageLf)) {
                        clients.remove(y);
                        display("Disconnected Client " + ct1.userName + " removed from list.");
                    }
                    display("<<"+ct1.userName+": "+WHISPER_RECEIVED+SEPARATOR+tocheck+SEPARATOR+w[1]);
                    found = true;
                    break;
                }
            }
            //return whether the user was found or not
            return found;
        }
        //If all clients are to receive the message
        else {
            String messageLf = time + " " + message + "\n";
            String[] user = message.split(" ");
            boolean success;
            for (int i = clients.size(); --i >= 0; ) {
                ClientThread ct = clients.get(i);
                //Display a user joining
                if(message.contains("joined")){
                    display(">>"+ct.userName+": "+USER_JOINED+SEPARATOR+user[2]);
                    ct.writeMsg(messageLf);
                }
                //Display a user leaving
                else if(message.contains("left")){
                    display(">>"+ct.userName+": "+USER_LEFT+SEPARATOR+user[2]);
                    ct.writeMsg(messageLf);
                }
                //Display the message
                else if(success=ct.writeMsg(messageLf)){
                    user[0] = user[0].replace(":", "");
                    display(">>"+ct.userName+": "+CHAT_RECEIVED+SEPARATOR+user[0]+SEPARATOR+w[1]);
                }
                //try to write to Client if it fails remove it from list since that client is no longer connected
                else if(!success){
                    clients.remove(i);
                    display("Disconnected Client " + ct.userName + " removed from list.");
                }
            }
        }
        //Return true was the broadcast is complete
        return true;
    }

    /**
     * Method for removing a user for the server
     * @param id-The position the client is at in the arrayList
     */
    synchronized void remove(int id) {

        String disconnectedClient = "";
        for (int i = 0; i < clients.size(); ++i) {
            ClientThread ct = clients.get(i);
            //Remove if found
            if (ct.id == id) {
                disconnectedClient = ct.getuserName();
                clients.remove(i);
                break;
            }
        }
        //Display sever protocol
        display(">>"+disconnectedClient+": "+DISCONNECTED);
        //Display the user leaving the chat room
        broadcast(notif + disconnectedClient + " has left the chat room." + notif, false, "");
    }

    /**
     * Main method for running the server
     * @param args
     */
    public static void main(String[] args) {
        // create a server object and start it
        ServerSide server = new ServerSide(PORT);
        server.start();
    }

    /**
     * Class that creates an instance of a thread for each client
     */
    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        //unique id easier for disconnecting a user
        int id;
        //userName of the Client
        String userName;
        //message object to receive message and its type
        ChatMessage cm;
        //timestamp
        String date;

        /**
         * Constructor that constructs a ClientThread
         * @param socket-The socket to get messages from client
         */
        public ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                //Obtain userName
                userName = (String) sInput.readObject();
                //Display protocol to connect user
                display("<<unknown user: "+CONNECT+SEPARATOR+userName);;
                broadcast(notif + userName + " has joined the chat room." + notif,false,"");
                //Display protocol of user being connected
                display(">>"+userName+": "+CONNECTED);
            } catch (IOException e) {
                display(">>"+FATAL_ERROR+SEPARATOR+e);
                return;
            } catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public String getuserName() {
            return userName;
        }

        /**
         * Run method for continuously reading input from user
         */
        public void run() {
            String[] command;
            boolean continueToRun = true;
            while (continueToRun) {
                // read a String (which is an object)
                try {
                    //Cast user input to a ChatMessage since its sent as one
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(">>"+userName+": " +FATAL_ERROR+SEPARATOR+e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                //Obtain the message from the ChatMessage object received
                String message = cm.getMessage();
                String temp = cm.getType();
                switch (cm.getType()) {
                    case SEND_CHAT:
                        //Display server protocol for sending a message to all clients
                        display("<<"+userName+": "+SEND_CHAT+SEPARATOR+message);
                        broadcast(userName + ": " + message,false,"");
                        break;
                    case DISCONNECT:
                        //Display server protocol for disconnecting a client
                        display("<<"+userName + ": "+DISCONNECT);
                        continueToRun = false;
                        break;
                    case LIST_USERS:
                        //Display server protocol for listing connected clients
                        display("<<"+userName+": "+LIST_USERS);
                        String users = USERS;
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        for (int i = 0; i < clients.size(); ++i) {
                            ClientThread ct = clients.get(i);
                            writeMsg((i + 1) + ") " + ct.userName + " since " + ct.date);
                            users += SEPARATOR+ct.userName;
                        }
                        //Display server protocol of all active clients
                        display(">>"+userName+": "+users);
                        break;
                    case SEND_WHISPER:
                        String user;
                        command = message.split(" ",2);
                        user = command[0];
                        //Display server protocol for sending a private message
                        display("<<"+userName+": "+SEND_WHISPER+SEPARATOR+user+SEPARATOR+command[1]);
                        if(!broadcast(user+" "+command[1],true, userName)){
                            String msg = notif + "Sorry. No such user exists." + notif;
                            writeMsg(msg);
                        }
                        else{
                            //Display server protocol for a private message that has been sent
                            display("<<"+userName+": "+WHISPER_SENT+SEPARATOR+user+SEPARATOR+command[1]);
                            writeMsg("You whispered to "+ user+": "+command[1]);
                        }
                        break;
                    case ERROR:
                        //Display server protocol for an unrecognized command
                        display(">>"+ERROR+": Error"+SEPARATOR+userName);
                        writeMsg("Command not recognized: Type \'/help\' for list of commands");
                        break;
                }
            }
            //If out of the loop then disconnected and remove from client list
            remove(id);
            close();
        }

        /**
         * Method that closes all the data streams
         */
        private void close() {
            try {
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {
            }
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {
            }

            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
            }
        }

        /**
         * Method for writing a string to the clients output stream
         * @param msg-The message being written
         * @return
         */
        private boolean writeMsg(String msg) {
            //Check to see if the client is connected
            if (!socket.isConnected()) {
                close();
                return false;
            }
            //Write the message
            try {
                sOutput.writeObject(msg);
            }
            catch (IOException e) {
                display(notif + "Error sending message to " + userName + notif);
                display(e.toString());
            }
            return true;
        }
    }
}

