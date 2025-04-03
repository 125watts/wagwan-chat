package com.wagwanchat;
import java.io.Serializable;

// Classe pour structurer les messages
public class ChatMessage implements Serializable {
    private String type;        // "LOGIN", "MESSAGE", "SYSTEM"
    private String username;    // Identifiant de l'utilisateur
    private String content;     // Contenu du message
    private long timestamp;     // Horodatage

    // Constructeurs, getters, setters...
    public ChatMessage(String type, String username, String content) {
        this.type = type;
        this.username = username;
        this.content = content;
    }

    public String getType() { return type; }
    public String getUsername() { return username; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return "Type: " + type + ", User: " + username + ", Message: " + content;
    }
    
} 