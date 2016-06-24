# EnMasseBench

The EnMasseBench or ebench for short is supposed to be a high performance client for EnMasse
benchmarking. It will send messages as fast as it can, and allows scaling the number of
senders/receivers to increase the load.

## Building

    gradle build

## Running
    tar xvf build/distributions/perf-tester.tar 
    ./perf-tester/bin/perf-tester -h 127.0.0.1 -c 1 -a amqp-test -p 5674 -r 60 -s 128  
