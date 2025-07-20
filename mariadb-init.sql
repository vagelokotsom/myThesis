USE thesisdb;

-- Drop existing tables
DROP TABLE IF EXISTS users;

-- Create users table with direct role column
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL
);

-- Insert a test teacher user (password: teacher123)
INSERT INTO users (username, password, email, role)
VALUES ('teacher', 
        '$2a$10$QLcHn3PWgcHq6aT7w3lKtOvZab6TDznqSNTW8nHZKIRWNqZtjhF26', 
        'teacher@example.com', 
        'ROLE_TEACHER');

-- Insert a test student user (password: student123)
INSERT INTO users (username, password, email, role)
VALUES ('student', 
        '$2a$10$2W7nw8M9DNNhHrN21RdMmOWcucjTTPTE5RlIgJ3/6qqbUEViurTgC', 
        'student@example.com', 
        'ROLE_STUDENT');
