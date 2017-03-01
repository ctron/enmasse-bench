#!/bin/sh
# Launches ebench from environment variables
ARGS=`echo $BENCH_ARGS | envsubst`
echo "Launching with args: $BENCH_ARGS (evaled $ARGS)"
./ebench-agent/bin/ebench-agent $ARGS
