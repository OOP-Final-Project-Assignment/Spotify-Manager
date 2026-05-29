package siu.k18.cntt.spotify.dao;

import siu.k18.cntt.spotify.utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class BaseDAO {
    
    // Shared database connection handler for all sub-DAOs (Inheritance principle)
    protected Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }
    
    // Abstract token method enforcing structural integrity for mapping tables (Abstraction principle)
    protected abstract String getTableName();
}