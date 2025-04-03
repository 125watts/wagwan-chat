package com.wagwanchat;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.mindrot.jbcrypt.BCrypt;

public class ChatServer {
    private static final int PORT = 3308;
    private static Map<String, PrintWriter> clientWriters = new HashMap<>(); // Username -> Writer
    private static Set<String> connectedClients = new HashSet<>();
    private static JTextArea logArea;
    private static JList<String> clientList;
    private static DefaultListModel<String> clientListModel;
    private static ServerSocket serverSocket;
    private static Connection dbConnection;
    
    public static void main(String[] args) {
        // Interface graphique
        JFrame frame = new JFrame("WagwanChat - Serveur");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeServer();
                System.exit(0);
            }
        });

        // Zone de logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);

        // Liste des clients
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setBorder(BorderFactory.createTitledBorder("Clients connectés"));
        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setPreferredSize(new Dimension(200, 0));

        frame.add(logScroll, BorderLayout.CENTER);
        frame.add(clientScroll, BorderLayout.EAST);
        frame.setVisible(true);

        logMessage("Le serveur démarre sur le port " + PORT + "...");

        try {
            serverSocket = new ServerSocket(PORT);
            initDatabase();
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                logMessage("Erreur: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Méthode pour diffuser un message à tous les clients
    private static void broadcast(String message) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters.values()) {
                writer.println(message);
            }
        }
    }

    private static void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static void updateClientList() {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String client : connectedClients) {
                clientListModel.addElement(client);
            }
        });
    }

    private static void closeServer() {
        try {
            logMessage("Fermeture du serveur...");
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters.values()) {
                    writer.close();
                }
                clientWriters.clear();
            }
            
            logMessage("Serveur fermé");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Initialisation de la base de données
    private static void initDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbConnection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/marley_db",
                "babylone_man",
                "jah"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Authentification d'un utilisateur
    private static boolean authenticateUser(String username, String password) {
        try {
            String query = "SELECT password FROM users WHERE username = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                return BCrypt.checkpw(password, hashedPassword);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Sauvegarde d'un message
    private static void saveMessage(String username, String content) {
        try {
            String query = "INSERT INTO messages (user_id, content) " +
                         "SELECT id, ? FROM users WHERE username = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, content);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // Attente de l'authentification
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ChatMessage loginMessage = (ChatMessage) ois.readObject();
                
                if (loginMessage.getType().equals("LOGIN")) {
                    if (authenticateUser(loginMessage.getUsername(), loginMessage.getContent())) {
                        this.username = loginMessage.getUsername();
                        // Suite du traitement...
                    } else {
                        // Échec de l'authentification
                        socket.close();
                        return;
                    }
                }

                // Traitement des messages
                String message;
                while ((message = in.readLine()) != null) {
                    saveMessage(username, message);
                    broadcast(username + ": " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}