
FROM ubuntu:latest
MAINTAINER Ian Robertson <iroberts@uw.edu>

RUN apt-get update && apt-get install -y --force-yes openjdk-8-jre-headless

ENV SCALA_VERSION 2.12
ENV KAFKA_VERSION 0.10.2.0
ENV KAFKA_HOME /tmp/kafka/kafka_${SCALA_VERSION}-${KAFKA_VERSION}
ENV ZK_SOCK localhost:2181


WORKDIR /tmp

# RUN curl -o kafka.tar.gz https://www-us.apache.org/dist/kafka/0.10.2.0/kafka_2.12-0.10.2.0.tgz
#ADD https://www-us.apache.org/dist/kafka/$KAFKA_VERSION/kafka_$SCALA_VERSION-$KAFKA_VERSION.tgz kafka.tar.gz
ADD kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz /tmp/kafka

# RUN openssl sha256 kafka.tgz | tee kafka.tgz.sha256

#FIXME
#RUN gnupg --verify

WORKDIR $KAFKA_HOME

ADD zookeeper.properties config/zookeeper.properties
#ADD server-0.properties server-1.properties server-2.properties server-3.properties config/
ADD server-*.properties config/
ADD kafka-cluster-start.sh bin/

RUN ["mkdir", "-p", "my-logs"]

VOLUME ["/tmp/kafka/my-logs"]

EXPOSE 2181 9092 9093 9094 9095

CMD ["bin/kafka-cluster-start.sh"]

# CMD ["/bin/bash"]


# vim: se makeprg=docker\ build\ .
