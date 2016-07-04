#!/bin/sh
# Launches ebench from environment variables
ARGS="-i $BENCH_INTERVAL"
echo "Launching with args: $ARGS"
./ebench-collector/bin/ebench-collector $ARGS
