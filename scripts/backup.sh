#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
database_file="$project_dir/backend/data/todo-db.mv.db"
backup_dir="$project_dir/backups"
if [ ! -f "$database_file" ]; then echo "No local database exists yet."; exit 1; fi
if ! command -v lsof >/dev/null; then
  echo "This safety check requires lsof. Stop the app and follow docs/DEPLOYMENT.md."; exit 1
fi
if lsof -t "$database_file" >/dev/null 2>&1; then
  echo "The database is in use. Stop the backend before taking a file backup."; exit 1
fi
umask 077
mkdir -p "$backup_dir"
backup_file="$backup_dir/todo-db-$(date +%Y%m%d-%H%M%S).mv.db"
if [ -e "$backup_file" ]; then echo "Backup name already exists. Try again in a second."; exit 1; fi
cp "$database_file" "$backup_file"
chmod 600 "$backup_file"
echo "Backup saved: $backup_file"
echo "This contains all accounts and private tasks. Keep it private."
