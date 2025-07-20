-- Update password hashes to correct BCrypt values
UPDATE users SET password = '$2a$10$QLcHn3PWgcHq6aT7w3lKtOvZab6TDznqSNTW8nHZKIRWNqZtjhF26' WHERE username = 'teacher';
UPDATE users SET password = '$2a$10$2W7nw8M9DNNhHrN21RdMmOWcucjTTPTE5RlIgJ3/6qqbUEViurTgC' WHERE username = 'student';
