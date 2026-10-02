#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"

# Prefer the supported bundled runtime when running inside Codex on macOS.
runtime_dir="$HOME/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin"
if [ -x "$runtime_dir/node" ]; then export PATH="$runtime_dir:$PATH"; fi
if ! command -v node >/dev/null; then
  echo "Node.js 24 is required. See README.md."; exit 1
fi
node -e 'if(Number(process.versions.node.split(".")[0])<24)process.exit(1)' || {
  echo "Use Node.js 24 or newer. See frontend/.nvmrc."; exit 1;
}
if [ -x /usr/libexec/java_home ]; then
  detected_java="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  if [ -n "$detected_java" ]; then export JAVA_HOME="$detected_java"; fi
fi
if ! command -v java >/dev/null; then echo "Java 17+ is required."; exit 1; fi
if command -v lsof >/dev/null; then
  for port in 8080 4200; do
    if lsof -n -iTCP:"$port" -sTCP:LISTEN -t >/dev/null; then
      echo "Port $port is already in use. Stop the existing app before launching."; exit 1
    fi
  done
fi
if [ ! -d frontend/node_modules ]; then (cd frontend && npm ci); fi
(cd backend && ./mvnw --batch-mode --no-transfer-progress -DskipTests package)
java_command=java
if [ -n "${JAVA_HOME:-}" ]; then java_command="$JAVA_HOME/bin/java"; fi
backend_pid=""
frontend_pid=""
cleanup() {
  trap - INT TERM EXIT
  [ -z "$frontend_pid" ] || kill "$frontend_pid" 2>/dev/null || true
  [ -z "$backend_pid" ] || kill "$backend_pid" 2>/dev/null || true
}
trap cleanup INT TERM EXIT
(cd backend && exec "$java_command" -jar target/daylight-api-1.0.0.jar) &
backend_pid=$!
(cd frontend && exec node node_modules/@angular/cli/bin/ng.js serve --host 127.0.0.1) &
frontend_pid=$!
echo "Daylight: http://127.0.0.1:4200 — press Ctrl+C to stop."
while kill -0 "$backend_pid" 2>/dev/null && kill -0 "$frontend_pid" 2>/dev/null; do sleep 1; done
echo "A service stopped. Check the output above."
exit 1
