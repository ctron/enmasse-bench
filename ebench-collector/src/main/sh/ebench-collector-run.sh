#!/bin/sh
# Launches ebench from environment variables
ARGS="-i $BENCH_INTERVAL -a $HOSTS"
echo "Launching with args: $ARGS"
while true
do
    ./ebench-collector/bin/ebench-collector $ARGS
done
