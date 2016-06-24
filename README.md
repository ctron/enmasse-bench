# Building

    gradle build

# Running
    tar xvf build/distributions/perf-tester.tar 
    ./perf-tester/bin/perf-tester -h 127.0.0.1 -c 1 -a amqp-test -p 5674 -r 60 -s 128  
