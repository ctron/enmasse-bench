#!/bin/sh
# Launches ebench from environment variables

./ebench/bin/ebench -c $BENCH_CLIENTS -h $MESSAGING_SERVICE_HOST -p $MESSAGING_SERVICE_PORT -a $BENCH_ADDRESS -d $BENCH_DURATION -s $BENCH_MSG_SIZE
