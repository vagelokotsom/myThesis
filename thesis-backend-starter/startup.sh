#!/bin/bash

# Startup script for SSH-enabled containers

# Start SSH service
service ssh start

# Set root password if provided
if [ ! -z "$ROOT_PASSWORD" ]; then
    echo "root:$ROOT_PASSWORD" | chpasswd
fi

# Create student users if provided
if [ ! -z "$SSH_USERS" ]; then
    IFS=',' read -ra USERS <<< "$SSH_USERS"
    for user_info in "${USERS[@]}"; do
        IFS=':' read -ra USER_PASS <<< "$user_info"
        username="${USER_PASS[0]}"
        password="${USER_PASS[1]}"
        
        # Create user with home directory
        useradd -m -s /bin/bash "$username"
        
        # Set password
        echo "$username:$password" | chpasswd
        
        # Add user to sudo group
        usermod -aG sudo "$username"
        
        # Create workspace directory for the user
        mkdir -p "/home/$username/workspace"
        chown "$username:$username" "/home/$username/workspace"
        
        echo "Created user: $username"
    done
fi

# Keep SSH server running
echo "SSH server started. Container ready for connections."

# Keep container running
tail -f /dev/null
