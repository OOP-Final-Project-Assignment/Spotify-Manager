package siu.k18.cntt.spotify.models;

public record Track(
    String trackId, 
    String trackName, 
    String artistName,
    String albumName, 
    String genreName, 
    int durationMs, 
    int popularity
) {}