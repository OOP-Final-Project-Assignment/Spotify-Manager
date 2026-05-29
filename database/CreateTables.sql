-- =======================================================
-- STEP 1: DROP OLD TABLES AND CREATE 6-TABLE OOP STANDARD STRUCTURE
-- =======================================================
DROP TABLE IF EXISTS Track_Artist;
DROP TABLE IF EXISTS Tracks;
DROP TABLE IF EXISTS Artists;
DROP TABLE IF EXISTS Albums;
DROP TABLE IF EXISTS Genres;
DROP TABLE IF EXISTS Users;

-- 1. Users Table 
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'Viewer'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Genres Table
CREATE TABLE Genres (
    genre_id INT AUTO_INCREMENT PRIMARY KEY,
    genre_name VARCHAR(100) NOT NULL UNIQUE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. Albums Table
CREATE TABLE Albums (
    album_id INT AUTO_INCREMENT PRIMARY KEY,
    album_name VARCHAR(255) NOT NULL UNIQUE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. Artist Table
CREATE TABLE Artists (
    artist_id INT AUTO_INCREMENT PRIMARY KEY,
    artist_name VARCHAR(255) NOT NULL UNIQUE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 5. Tracks Table
CREATE TABLE Tracks (
    track_id VARCHAR(50) PRIMARY KEY,
    track_name VARCHAR(255) NOT NULL,
    album_id INT,
    genre_id INT,
    popularity INT,
    duration_ms INT,
    explicit BOOLEAN,
    danceability DOUBLE,
    energy DOUBLE,
    tempo DOUBLE,
    FOREIGN KEY (album_id) REFERENCES Albums(album_id) ON DELETE SET NULL,
    FOREIGN KEY (genre_id) REFERENCES Genres(genre_id) ON DELETE SET NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 6. Track_Artist Table
CREATE TABLE Track_Artist (
    track_id VARCHAR(50),
    artist_id INT,
    PRIMARY KEY (track_id, artist_id),
    FOREIGN KEY (track_id) REFERENCES Tracks(track_id) ON DELETE CASCADE,
    FOREIGN KEY (artist_id) REFERENCES Artists(artist_id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =======================================================
-- STEP 2: EXTRACT AND NORMALIZE DATA FROM SPOTIFY_RAW TO 6 TABLES
-- =======================================================

INSERT IGNORE INTO Genres (genre_name)
SELECT DISTINCT track_genre FROM spotify_raw WHERE track_genre IS NOT NULL;

INSERT IGNORE INTO Albums (album_name)
SELECT DISTINCT album_name FROM spotify_raw WHERE album_name IS NOT NULL;

INSERT IGNORE INTO Artists (artist_name)
SELECT DISTINCT artists FROM spotify_raw WHERE artists IS NOT NULL;

INSERT IGNORE INTO Tracks (track_id, track_name, album_id, genre_id, popularity, duration_ms, explicit, danceability, energy, tempo)
SELECT DISTINCT 
    r.track_id, 
    r.track_name, 
    a.album_id, 
    g.genre_id, 
    r.popularity, 
    r.duration_ms, 
    IF(r.explicit = 'True' OR r.explicit = '1', 1, 0), 
    r.danceability, 
    r.energy, 
    r.tempo
FROM spotify_raw r
LEFT JOIN Albums a ON r.album_name = a.album_name
LEFT JOIN Genres g ON r.track_genre = g.genre_name
WHERE r.track_id IS NOT NULL;

INSERT IGNORE INTO Track_Artist (track_id, artist_id)
SELECT DISTINCT r.track_id, a.artist_id
FROM spotify_raw r
JOIN Artists a ON r.artists = a.artist_name;

-- CREATE ADMIN ACCOUNT, PASSWORDD 1234
INSERT INTO Users (username, password_hash, role) 
VALUES ('admin', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'Admin');