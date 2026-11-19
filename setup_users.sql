-- Local TEST database created only for this assignment. Never run this on a real system.
CREATE DATABASE IF NOT EXISTS busdb_test;
USE busdb_test;

DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id  VARCHAR(30) PRIMARY KEY,
    pwd VARCHAR(60) NOT NULL          -- plain text ONLY because this is a throw-away demo table
);
INSERT INTO users VALUES ('admin',  'S3cret#99');
INSERT INTO users VALUES ('arun',   'bus@2026');
