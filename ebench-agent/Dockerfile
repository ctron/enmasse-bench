FROM java:8

ADD build/distributions/ebench-agent.tar /
ADD src/main/sh/ebench-agent-run.sh /

EXPOSE 8080

CMD ["/ebench-agent-run.sh"]
