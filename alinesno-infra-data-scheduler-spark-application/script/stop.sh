#!/bin/bash
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
PIDFILE="$APP_HOME/run/app.pid"

if [ ! -f "$PIDFILE" ]; then
  echo "Not running (no pid file)"
  exit 0
fi

PID=$(cat "$PIDFILE")
kill $PID
sleep 5
if kill -0 $PID >/dev/null 2>&1; then
  echo "Process still alive, force kill"
  kill -9 $PID
fi
rm -f "$PIDFILE"
echo "Stopped"