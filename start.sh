#!/bin/sh

echo "--- Starting: $NODE_ID ---"

java -jar /app/app.jar > /app/data/"$NODE_ID".log 2>&1