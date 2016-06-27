FROM java:8

ADD build/distributions/ebench.tar /
ADD src/main/sh/ebench-env-run.sh /

CMD ["/ebench-env-run.sh"]
