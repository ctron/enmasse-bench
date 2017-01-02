#!/bin/sh
# Launches ebench from environment variables
ARGS="-s $BENCH_SENDERS -r $BENCH_RECEIVERS -h $MESSAGING_SERVICE_HOST:$MESSAGING_SERVICE_PORT -a $BENCH_ADDRESS -d $BENCH_DURATION -m $BENCH_MSG_SIZE -f none"
if [ "$BENCH_WAIT_TIME" != "" ]
then
    ARGS="$ARGS -w $BENCH_WAIT_TIME"
fi

if [ "$BENCH_SPLIT_CLIENTS" != "" ]
then
    ARGS="$ARGS -c"
fi

echo "Launching with args: $ARGS"
./ebench-agent/bin/ebench-agent $ARGS
