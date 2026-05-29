USE spotify_db;

CREATE TABLE spotify_raw (
    `Unnamed: 0` INT,
    track_id VARCHAR(100),
    artists TEXT,
    album_name TEXT,
    track_name TEXT,
    popularity INT,
    duration_ms INT,
    explicit VARCHAR(10),
    danceability DOUBLE,
    energy DOUBLE,
    `key` INT,
    loudness DOUBLE,
    `mode` INT,
    speechiness DOUBLE,
    acousticness DOUBLE,
    instrumentalness DOUBLE,
    liveness DOUBLE,
    valence DOUBLE,
    tempo DOUBLE,
    time_signature INT,
    track_genre VARCHAR(100)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

