#!/bin/bash
set -euo pipefail

# Start SSH daemon for container templates that expect SSH access.

# Root password comes from ROOT_PASSWORD env (default set via Config/Service).
: "${ROOT_PASSWORD:=rootpass123}"

# Student workspace user/password injected by backend per container start.
: "${WORKSPACE_USER:=student}"
: "${STUDENT_PASSWORD:=${ROOT_PASSWORD}}"

echo "root:${ROOT_PASSWORD}" | chpasswd

if ! id "${WORKSPACE_USER}" >/dev/null 2>&1; then
  useradd -m -s /bin/bash "${WORKSPACE_USER}"
fi
echo "${WORKSPACE_USER}:${STUDENT_PASSWORD}" | chpasswd

mkdir -p /workspace
chown "${WORKSPACE_USER}:${WORKSPACE_USER}" /workspace

service ssh start >/dev/null 2>&1 || true
exec /usr/sbin/sshd -D -e
