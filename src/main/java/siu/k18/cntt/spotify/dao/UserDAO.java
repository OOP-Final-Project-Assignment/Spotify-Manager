package siu.k18.cntt.spotify.dao;

import siu.k18.cntt.spotify.models.User;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO extends BaseDAO { 

    @Override
    protected String getTableName() {
        return "Users"; 
    }

    // Cryptographic hash function using SHA-256 for secure password storage
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    // Retrieves all registered users for Admin management dashboard
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + getTableName();
        try (Connection conn = getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("role")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    // Updates user role and optionally resets password
    public boolean updateUser(int userId, String newRole, String newPassword) {
        boolean updatePassword = newPassword != null && !newPassword.trim().isEmpty();
        String sql = updatePassword 
                ? "UPDATE " + getTableName() + " SET role = ?, password_hash = ? WHERE user_id = ?" 
                : "UPDATE " + getTableName() + " SET role = ? WHERE user_id = ?";
                
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newRole);
            if (updatePassword) {
                pstmt.setString(2, hashPassword(newPassword.trim()));
                pstmt.setInt(3, userId);
            } else {
                pstmt.setInt(2, userId);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM " + getTableName() + " WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // Validates credentials during login process
    public Optional<User> authenticate(String username, String password) {
        String hashedInputPassword = hashPassword(password); 
        String sql = "SELECT * FROM " + getTableName() + " WHERE username = ? AND password_hash = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedInputPassword);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("role"));
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty(); 
    }

    // Registers a new user with default 'User' permission role
    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO " + getTableName() + " (username, password_hash, role) VALUES (?, ?, 'User')";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            pstmt.setString(2, hashPassword(password));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}