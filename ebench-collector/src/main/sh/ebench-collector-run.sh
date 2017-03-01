#!/bin/sh
# Launches ebench from environment variables
echo "Launching with args: $BENCH_ARGS"
./ebench-collector/bin/ebench-collector $BENCH_ARGS
