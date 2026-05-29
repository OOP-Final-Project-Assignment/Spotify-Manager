package siu.k18.cntt.spotify.dao;

import siu.k18.cntt.spotify.models.Track;
import siu.k18.cntt.spotify.utils.DatabaseConnection;
import siu.k18.cntt.spotify.models.Artist;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TrackDAO implements IGenericDAO<Track> {
    
    @Override
    public List<Track> getTracks(int offset, int limit, String searchCol, String keyword) {
        List<Track> tracks = new ArrayList<>();
        
        // Fetches track details and aggregates all associated artists using a subquery on the Track_Artist junction table
        String sql = "SELECT t.track_id, t.track_name, a.album_name, g.genre_name, t.duration_ms, t.popularity, " +
                     "  (SELECT GROUP_CONCAT(art.artist_name SEPARATOR ', ') " +
                     "   FROM Track_Artist ta JOIN Artists art ON ta.artist_id = art.artist_id " +
                     "   WHERE ta.track_id = t.track_id) AS artist_name " +
                     "FROM Tracks t " +
                     "LEFT JOIN Albums a ON t.album_id = a.album_id " +
                     "LEFT JOIN Genres g ON t.genre_id = g.genre_id ";

        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        if (hasSearch) {
            if (searchCol.equals("All")) {
                sql += "WHERE t.track_name LIKE ? OR a.album_name LIKE ? OR g.genre_name LIKE ? OR t.track_id LIKE ? ";
            } else if (searchCol.equals("Track Name")) sql += "WHERE t.track_name LIKE ? ";
              else if (searchCol.equals("Album")) sql += "WHERE a.album_name LIKE ? ";
              else if (searchCol.equals("Genre")) sql += "WHERE g.genre_name LIKE ? ";
              else if (searchCol.equals("ID")) sql += "WHERE t.track_id LIKE ? ";
        }
        sql += "LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (hasSearch) {
                String pattern = "%" + keyword.trim() + "%";
                if (searchCol.equals("All")) {
                    pstmt.setString(paramIndex++, pattern); pstmt.setString(paramIndex++, pattern);
                    pstmt.setString(paramIndex++, pattern); pstmt.setString(paramIndex++, pattern);
                } else {
                    pstmt.setString(paramIndex++, pattern);
                }
            }
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tracks.add(new Track(
                        rs.getString("track_id"), rs.getString("track_name"),
                        // Resolves the concatenated artist names or defaults to 'Unknown Artist'
                        rs.getString("artist_name") == null ? "Unknown Artist" : rs.getString("artist_name"), 
                        rs.getString("album_name"), rs.getString("genre_name"),
                        rs.getInt("duration_ms"), rs.getInt("popularity")
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return tracks;
    }

    @Override
    public boolean insert(Track track) {
        String insertTrackSql = "INSERT INTO Tracks (track_id, track_name, duration_ms, popularity) VALUES (?, ?, ?, ?)";
        String insertArtistSql = "INSERT IGNORE INTO Artists (artist_name) VALUES (?)";
        String linkArtistSql = "INSERT IGNORE INTO Track_Artist (track_id, artist_id) " +
                               "SELECT ?, artist_id FROM Artists WHERE artist_name = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Enables database transactions to ensure data integrity during multiple insertions
            conn.setAutoCommit(false); 
            
            try {
                // Step 1: Insert core track details into the Tracks table
                try (PreparedStatement pstmt = conn.prepareStatement(insertTrackSql)) {
                    pstmt.setString(1, track.trackId()); pstmt.setString(2, track.trackName());
                    pstmt.setInt(3, track.durationMs()); pstmt.setInt(4, track.popularity());
                    pstmt.executeUpdate();
                }
                
                // Step 2: Automatically register the artist (if provided) and establish the many-to-many relationship
                if (track.artistName() != null && !track.artistName().trim().isEmpty()) {
                    try (PreparedStatement pstmt = conn.prepareStatement(insertArtistSql)) {
                        pstmt.setString(1, track.artistName().trim());
                        pstmt.executeUpdate();
                    }
                    try (PreparedStatement pstmt = conn.prepareStatement(linkArtistSql)) {
                        pstmt.setString(1, track.trackId());
                        pstmt.setString(2, track.artistName().trim());
                        pstmt.executeUpdate();
                    }
                }
                
                // Commits the transaction if all operations succeed
                conn.commit(); 
                return true;
            } catch (SQLException ex) {
                // Rolls back the transaction in case of failure to prevent partial data (orphan records)
                conn.rollback(); 
                ex.printStackTrace();
                return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public boolean update(Track track) {
        String updateTrackSql = "UPDATE Tracks SET track_name = ?, duration_ms = ?, popularity = ? WHERE track_id = ?";
        String insertArtistSql = "INSERT IGNORE INTO Artists (artist_name) VALUES (?)";
        String deleteOldLink = "DELETE FROM Track_Artist WHERE track_id = ?";
        String linkArtistSql = "INSERT IGNORE INTO Track_Artist (track_id, artist_id) " +
                               "SELECT ?, artist_id FROM Artists WHERE artist_name = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step 1: Update the core track metrics
                try (PreparedStatement pstmt = conn.prepareStatement(updateTrackSql)) {
                    pstmt.setString(1, track.trackName()); pstmt.setInt(2, track.durationMs());
                    pstmt.setInt(3, track.popularity()); pstmt.setString(4, track.trackId());
                    pstmt.executeUpdate();
                }
                
                // Step 2: Re-establish artist relationships by clearing old links and inserting new ones
                try (PreparedStatement pstmt = conn.prepareStatement(deleteOldLink)) {
                    pstmt.setString(1, track.trackId()); pstmt.executeUpdate();
                }
                
                if (track.artistName() != null && !track.artistName().trim().isEmpty()) {
                    try (PreparedStatement pstmt = conn.prepareStatement(insertArtistSql)) {
                        pstmt.setString(1, track.artistName().trim()); pstmt.executeUpdate();
                    }
                    try (PreparedStatement pstmt = conn.prepareStatement(linkArtistSql)) {
                        pstmt.setString(1, track.trackId()); pstmt.setString(2, track.artistName().trim());
                        pstmt.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) { conn.rollback(); ex.printStackTrace(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public boolean delete(String trackId) {
        String sql = "DELETE FROM Tracks WHERE track_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trackId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public int getTotalTracks(String searchCol, String keyword) {
        String sql = "SELECT COUNT(*) FROM Tracks t " +
                     "LEFT JOIN Albums a ON t.album_id = a.album_id " +
                     "LEFT JOIN Genres g ON t.genre_id = g.genre_id ";
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        
        // Dynamically appends WHERE clauses based on the selected search category
        if (hasSearch) {
             if (searchCol.equals("All")) {
                sql += "WHERE t.track_name LIKE ? OR a.album_name LIKE ? OR g.genre_name LIKE ? OR t.track_id LIKE ? ";
            } else if (searchCol.equals("Track Name")) sql += "WHERE t.track_name LIKE ? ";
              else if (searchCol.equals("Album")) sql += "WHERE a.album_name LIKE ? ";
              else if (searchCol.equals("Genre")) sql += "WHERE g.genre_name LIKE ? ";
              else if (searchCol.equals("ID")) sql += "WHERE t.track_id LIKE ? ";
        }
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hasSearch) {
                String pattern = "%" + keyword.trim() + "%";
                 if (searchCol.equals("All")) {
                    pstmt.setString(1, pattern); pstmt.setString(2, pattern);
                    pstmt.setString(3, pattern); pstmt.setString(4, pattern);
                } else pstmt.setString(1, pattern);
            }
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // Fetches top 5 tracks based on popularity metrics for the Bar Chart visualization
    public List<Track> getTop5PopularTracks() {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT t.track_id, t.track_name, a.album_name, g.genre_name, t.duration_ms, t.popularity, " +
                     "  (SELECT GROUP_CONCAT(art.artist_name SEPARATOR ', ') FROM Track_Artist ta JOIN Artists art ON ta.artist_id = art.artist_id WHERE ta.track_id = t.track_id) AS artist_name " +
                     "FROM Tracks t LEFT JOIN Albums a ON t.album_id = a.album_id LEFT JOIN Genres g ON t.genre_id = g.genre_id " +
                     "ORDER BY t.popularity DESC LIMIT 5";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                tracks.add(new Track(rs.getString("track_id"), rs.getString("track_name"), rs.getString("artist_name"), rs.getString("album_name"), rs.getString("genre_name"), rs.getInt("duration_ms"), rs.getInt("popularity")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return tracks;
    }
}