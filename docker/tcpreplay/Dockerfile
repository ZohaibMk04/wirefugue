FROM alpine:latest
MAINTAINER Ian Robertson <iroberts@uw.edu>

ENV TCPREPLAY_VERSION 4.2.5
ENV CAPTURE_FILE smallFlows.pcap

RUN apk update
RUN apk upgrade

RUN apk add wget ca-certificates build-base libpcap-dev
RUN update-ca-certificates
RUN mkdir /tmp/tcpreplay-build

WORKDIR /tmp/tcpreplay-build

RUN wget https://github.com/appneta/tcpreplay/releases/download/v4.2.5/tcpreplay-4.2.5.tar.gz
RUN tar xvf tcpreplay-$TCPREPLAY_VERSION.tar.gz

WORKDIR /tmp/tcpreplay-build/tcpreplay-$TCPREPLAY_VERSION

RUN ./configure && make && make install

WORKDIR /root

RUN rm -rf /tmp/tcpreplay-build

RUN wget -O smallFlows.pcap https://s3.amazonaws.com/tcpreplay-pcap-files/smallFlows.pcap

#ADD smallFlows.pcap .

CMD ["tcpreplay", "-i", "eth0", "-t", "-K", "smallFlows.pcap"]

# CMD ["/bin/ash"]

# vim: se makeprg=docker\ build\ .
