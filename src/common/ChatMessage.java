package common;

import java.io.*;

/**
 * Class for defining the type of message that is being sent to the server.
 * Author: Nick Vaccarello
 * email: nwv4110@rit.edu
 */
public class ChatMessage implements Serializable {

    private String type;
    private String message;

    /**
     * Constructs the ChatMessage based on the type and the message
     * @param type
     * @param message
     */
    public ChatMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    /**
     * Gets the type of message
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the message
     */
    public String getMessage() {
        return message;
    }
}
