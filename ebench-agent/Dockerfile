FROM fedora:25

RUN dnf -y install java-1.8.0-openjdk gettext && dnf clean all -y
ADD build/distributions/ebench-agent.tar /
ADD src/main/sh/ebench-agent-run.sh /

EXPOSE 8080

CMD ["/ebench-agent-run.sh"]
