FROM alpine:latest
MAINTAINER Ian Robertson <iroberts@uw.edu>

ENV TCPREPLAY_VERSION 4.2.5
ENV CAPTURE_FILE smallFlows.pcap

RUN apk update
RUN apk upgrade

RUN apk add tcpdump

CMD ["tcpdump", "-vvni", "eth0"]

# vim: se makeprg=docker\ build\ .
