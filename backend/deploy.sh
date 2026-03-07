#!/bin/bash

# Configuration
PROJECT_DIR="/home/maintainer/Documents/scuola/tps/RPiChat/backend/app"
VENV_DIR="/home/maintainer/Documents/scuola/tps/RPiChat/backend/.venv"
PID_FILE="/home/maintainer/Documents/scuola/tps/RPiChat/backend/server.pid"
LOG_FILE="/home/maintainer/Documents/scuola/tps/RPiChat/backend/server.log"
PORT=8000
HOST="0.0.0.0"

cd $(dirname $0)
BASE_DIR=$(pwd)

function start_server() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null; then
            echo "Server is already running (PID: $PID)."
            return
        else
            echo "Found stale PID file. Cleaning up..."
            rm "$PID_FILE"
        fi
    fi

    echo "Starting RPiChat server..."
    
    if [ -d "$VENV_DIR" ]; then
        source "$VENV_DIR/bin/activate"
    else
        echo "Virtual environment not found at $VENV_DIR. Creating one..."
        python3 -m venv "$VENV_DIR"
        source "$VENV_DIR/bin/activate"
        echo "Installing requirements..."
        if [ -f "requirements.txt" ]; then
            pip install -r requirements.txt
        else
            echo "Warning: requirements.txt not found. Dependencies may be missing."
        fi
    fi

    # Start the server in the background
    nohup uvicorn app.main:app --host $HOST --port $PORT > "$LOG_FILE" 2>&1 &
    
    PID=$!
    echo $PID > "$PID_FILE"
    echo "Server started gracefully in the background (PID: $PID)."
    echo "Logs are being written to $LOG_FILE"
    echo "Access the API at http://$HOST:$PORT"
}

function stop_server() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null; then
            echo "Stopping server (PID: $PID)..."
            kill $PID
            rm "$PID_FILE"
            echo "Server stopped."
        else
            echo "Server is not running (stale PID file found and removed)."
            rm "$PID_FILE"
        fi
    else
        # Fallback check if PID file is missing but server is running
        PIDS=$(pgrep -f "uvicorn app.main:app")
        if [ ! -z "$PIDS" ]; then
            echo "PID file missing, but found running uvicorn processes. Killing them..."
            kill $PIDS
            echo "Server stopped."
        else
            echo "Server is not running."
        fi
    fi
}

function status_server() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null; then
            echo "Server is running (PID: $PID) on http://$HOST:$PORT"
        else
            echo "Server is offline (stale PID file)."
        fi
    else
        echo "Server is currently stopped."
    fi
}

case "$1" in
    start)
        start_server
        ;;
    stop)
        stop_server
        ;;
    restart)
        stop_server
        sleep 2
        start_server
        ;;
    status)
        status_server
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
