package siu.k18.cntt.spotify.dao;

import siu.k18.cntt.spotify.models.Artist;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ArtistDAO extends BaseDAO { // Inherits Abstract Class

    @Override
    protected String getTableName() {
        return "Artists"; // Define target table name
    }

    // Fetches a paginated list of artists, with optional keyword searching
    public List<Artist> getArtists(int offset, int limit, String keyword) {
        List<Artist> artists = new ArrayList<>();
        String sql = "SELECT * FROM " + getTableName() + " "; 
        
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        if (hasSearch) {
            sql += "WHERE artist_name LIKE ? ";
        }
        sql += "ORDER BY artist_id DESC LIMIT ? OFFSET ?";

        try (Connection conn = getConnection(); // Calls inherited method
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (hasSearch) {
                pstmt.setString(paramIndex++, "%" + keyword.trim() + "%");
            }
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    artists.add(new Artist(rs.getInt("artist_id"), rs.getString("artist_name")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return artists;
    }

    // Returns total count of artists for pagination UI calculations
    public int getTotalArtists(String keyword) {
        String sql = "SELECT COUNT(*) FROM " + getTableName() + " ";
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        if (hasSearch) {
            sql += "WHERE artist_name LIKE ? ";
        }

        try (Connection conn = getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hasSearch) {
                pstmt.setString(1, "%" + keyword.trim() + "%");
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public boolean insert(Artist artist) {
        String sql = "INSERT INTO " + getTableName() + " (artist_name) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artist.artistName());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Artist artist) {
        String sql = "UPDATE " + getTableName() + " SET artist_name = ? WHERE artist_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artist.artistName());
            pstmt.setInt(2, artist.artistId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int artistId) {
        String sql = "DELETE FROM " + getTableName() + " WHERE artist_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, artistId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Aggregates track popularity to find top 5 artists for pie chart visualization
    public java.util.Map<String, Integer> getTop5PopularArtists() {
        java.util.Map<String, Integer> artistStats = new java.util.LinkedHashMap<>();
        String sql = "SELECT a.artist_name, SUM(t.popularity) as total_popularity " +
                     "FROM " + getTableName() + " a " +
                     "JOIN Track_Artist ta ON a.artist_id = ta.artist_id " +
                     "JOIN Tracks t ON ta.track_id = t.track_id " +
                     "GROUP BY a.artist_id, a.artist_name " +
                     "ORDER BY total_popularity DESC LIMIT 5";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                artistStats.put(rs.getString("artist_name"), rs.getInt("total_popularity"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return artistStats;
    }
}