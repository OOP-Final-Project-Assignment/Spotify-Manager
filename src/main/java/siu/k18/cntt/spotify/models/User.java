package siu.k18.cntt.spotify.models;

public record User(
    int userId, 
    String username, 
    String passwordHash, 
    String role
) {}