package siu.k18.cntt.spotify.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Singleton Instance
    private static DatabaseConnection instance;
    private Connection connection;
    
    // Database credentials
    private final String URL = "jdbc:mysql://localhost:3306/spotify_db";
    private final String USER = "root";
    
    // TODO: Update your local MySQL password here if needed
    private final String PASSWORD = "1234"; 

    // Private constructor prevents external instantiation (Singleton pattern)
    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    // Global access point for the Singleton instance
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        } else {
            try {
                // Reconnect if the connection was dropped or closed
                if (instance.getConnection().isClosed()) {
                    instance = new DatabaseConnection();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    // Retrieve the active connection object
    public Connection getConnection() {
        return connection;
    }
}