package com.wagwanchat;
import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatClient {
    private String username;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JFrame frame;
    private JTextArea textArea;
    private JTextField textField;
    private ObjectOutputStream objectOutputStream;

    public static void main(String[] args) {
        new ChatClient().start();
    }

    public void start() {
        JFrame loginFrame = new JFrame("WagwanChat - Login");
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JButton loginButton = new JButton("Login");
        
        JPanel panel = new JPanel();
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(loginButton);
        
        loginFrame.add(panel);
        loginFrame.pack();
        loginFrame.setVisible(true);
        
        loginButton.addActionListener(e -> {
            if (login(usernameField.getText(), new String(passwordField.getPassword()))) {
                this.username = usernameField.getText();
                loginFrame.dispose();
                showChatWindow();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Login failed!");
            }
        });
    }

    public boolean login(String username, String password) {
        try {
            // Établir la connexion avec le serveur
            socket = new Socket("127.0.0.1", 3308); // Change l'adresse et le port si nécessaire
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
            // **Ajoute cette ligne pour initialiser `out`**
            out = new PrintWriter(socket.getOutputStream(), true);
    
            // Création et envoi du message de login
            ChatMessage loginMessage = new ChatMessage("LOGIN", username, password);
            objectOutputStream.writeObject(loginMessage);
            objectOutputStream.flush();
    
            // Lire la réponse du serveur
            String response = in.readLine();
            return "LOGIN_SUCCESS".equals(response);
    
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void handleMessage(String message) {
        if (message.startsWith(username + ": ")) {
            // C'est notre message
            String actualMessage = message.substring((username + ": ").length());
            textArea.append("Moi: " + actualMessage + "\n");
        } else {
            // Message d'un autre utilisateur
            textArea.append(message + "\n");
        }
    }

    private void showChatWindow() {
        frame = new JFrame("WagwanChat - " + username);
        textArea = new JTextArea();
        textField = new JTextField();

        textArea.setEditable(false);
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.add(textField, BorderLayout.SOUTH);

        textField.addActionListener(e -> {
            String message = textField.getText();

            if (!message.trim().isEmpty()) {
                try {
                    ChatMessage chatMessage = new ChatMessage("MESSAGE", username, message);
                    objectOutputStream.writeObject(chatMessage);
                    objectOutputStream.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                textField.setText("");
            }
        });

        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message); // Affiche le message reçu dans le chat
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        
    }
}