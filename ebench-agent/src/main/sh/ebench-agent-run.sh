#!/bin/sh
# Launches ebench from environment variables
echo "Launching with args: $BENCH_ARGS"
./ebench-agent/bin/ebench-agent $BENCH_ARGS
