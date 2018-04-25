package client;

import common.ChatMessage;

import java.net.*;
import java.io.*;
import java.util.*;


/**
 * The ClientSide used to handle the clients input and communicated with the server
 * Author: Nick Vaccarello
 * email: nwv4110@rit.edu
 */
public class ClientSide implements common.ChatterboxProtocol {
    private String notif = " *** ";
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;
    private String server, userName;
    private int port;

    /**
     * Constructor for creating a client with their userName, server, and port
     * @param server
     * @param port
     * @param userName
     */
    public ClientSide(String server, int port, String userName) {
        this.server = server;
        this.port = port;
        this.userName = userName;
    }

    /**
     * Method for starting the chat by trying to establish connection to the server
     * @return
     */
    public boolean start() {
        try {
            socket = new Socket(server, port);
        }
        //Error if they can't connect to the server
        catch(Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }
        //Display the connection status of the client
        String msg = "ChatterboxClient connection received from " + socket.getInetAddress()+"\nChatterbox server port: "
                + socket.getPort();
        display(msg);

        //Create data streams for reading and writing
        try
        {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        //Creates a ListenFromServer thread to begin to listen for messages
        new ListenFromServer().start();
        //Sends the userName as a string, the rest of the writing will be objects
        try
        {
            sOutput.writeObject(userName);
        }
        catch (IOException eIO) {
            //Error in case login fails
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        //Return true saying that login was a success
        return true;
    }

    /**
     * Display method for the client console
     * @param msg-The message being displayed
     */
    private void display(String msg) {
        System.out.println(msg);
    }

    /**
     * Method for sending a method to the server
     * @param msg-The message being sent
     */
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    /**
     * Disconnect method that closes all the data streams
     */
    private void disconnect() {
        try {
            if(sInput != null) sInput.close();
        }
        catch(Exception e) {}
        try {
            if(sOutput != null) sOutput.close();
        }
        catch(Exception e) {}
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {}

    }

    /**
     * Main method for running the client
     * @param args
     */
    public static void main(String[] args) {
        String serverAddress = "localhost";
        Scanner scan = new Scanner(System.in);

        System.out.print("Enter the userName: ");
        String userName = scan.nextLine();
        ClientSide client = new ClientSide(serverAddress, PORT, userName);
        //Try and connect to the server
        if(!client.start())
            return;

        System.out.println("\nHello.! Welcome to ChatterBox "+userName+"! Type \'/help\' to see a list of commands ");

        //Loop for getting client input that will be handled
        while(true) {
            String[] command;
            System.out.print("> ");
            String msg = scan.nextLine();
            //Display commands if client types /help
            if(msg.equals("/help")){
                System.out.println("/help - displays this message");
                System.out.println("/quit - quit Chatterbox");
                System.out.println("/c <message> - send a message to all connected clients");
                System.out.println("/w <recipient> <message> - send a private message to the recipient");
                System.out.println("/list - display a list of currently connected users");
            }
            //String for additional input if the user wishes to quit
            String answer = "";
            if(msg.equals("/quit")){
                System.out.print("Are you sure (y/n)\n>");
                answer = scan.nextLine();
            }
            //Quit/Disconnect if users answers y(yes) for quiting
            if(answer.equalsIgnoreCase("y")) {
                client.sendMessage(new ChatMessage(DISCONNECT, ""));
                break;
            }
            else if(answer.equalsIgnoreCase("n")){
                //Do nothing and accept more commands from user
            }
            //For displaying connected users
            else if(msg.equalsIgnoreCase("/list")) {
                client.sendMessage(new ChatMessage(LIST_USERS, ""));
            }
            //Sending a message to all users
            else if(msg.contains("/c")) {
                msg = msg.split("/c")[1];
                client.sendMessage(new ChatMessage(SEND_CHAT, msg));
            }
            //For private messaging
            else if(msg.contains("/w")&& msg.split(" ", 3).length>2){
                command = msg.split(" ", 2);
                msg = command[1];
                client.sendMessage(new ChatMessage(SEND_WHISPER, msg));
            }
            //It isn't an acceptable command
            else{
                client.sendMessage(new ChatMessage(ERROR, msg));
            }

        }
        //Close resources
        scan.close();
        //Disconnect the client
        client.disconnect();
    }

    /**
     * Class for listening for messages being sent in to the client
     */
    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {
                    //Read the message
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                    System.out.print("> ");
                }
                catch(IOException e) {
                    //Display the connection was successfully closed
                    display(notif +" Connection successfully closed: Goodbye "+ notif);
                    break;
                }
                catch(ClassNotFoundException e2) {

                }
            }
        }
    }
}
