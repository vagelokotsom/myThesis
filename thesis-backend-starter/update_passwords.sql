-- Update user passwords with correct BCrypt hashes
USE thesisdb;

-- Drop existing users if they exist and recreate with correct hashes
DELETE FROM users WHERE username IN ('teacher', 'student');

-- Insert users with correct BCrypt hashes
INSERT INTO users (username, password, email, role)
VALUES 
    ('teacher', '$2a$10$QLcHn3PWgcHq6aT7w3lKtOvZab6TDznqSNTW8nHZKIRWNqZtjhF26', 'teacher@example.com', 'ROLE_TEACHER'),
    ('student', '$2a$10$2W7nw8M9DNNhHrN21RdMmOWcucjTTPTE5RlIgJ3/6qqbUEViurTgC', 'student@example.com', 'ROLE_STUDENT');

-- Verify the data
SELECT username, LEFT(password, 30) as password_hash, email, role FROM users;
