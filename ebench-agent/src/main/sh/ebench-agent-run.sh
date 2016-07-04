#!/bin/sh
# Launches ebench from environment variables
ARGS="-c $BENCH_CLIENTS -h $MESSAGING_SERVICE_HOST -p $MESSAGING_SERVICE_PORT -a $BENCH_ADDRESS -d $BENCH_DURATION -s $BENCH_MSG_SIZE -m collector"
if [ "$BENCH_WAIT_TIME" != "" ]
then
    ARGS="$ARGS -w $BENCH_WAIT_TIME"
fi
echo "Launching with args: $ARGS"
while true
do
    ./ebench-agent/bin/ebench-agent $ARGS
    sleep 2
done
