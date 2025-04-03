package com.wagwanchat;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    public static void main(String[] args) {
        String plainPassword = "";  // Remplace avec le vrai mot de passe
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        System.out.println("Nouveau mot de passe haché : " + hashedPassword);
    }
}