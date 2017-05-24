#!/bin/bash

echo "KAFKA_HOME=$KAFKA_HOME"
if [[ -z "$KAFKA_HOME" ]]; then
    echo "KAFKA_HOME not set."
    exit 2
fi

cd $KAFKA_HOME

PIDS=

stop_all() {
    trap "exit 0" HUP INT QUIT TERM
    bin/kafka-server-stop.sh
    sleep 15
    bin/zookeeper-server-stop.sh
    wait
}

trap stop_all HUP INT QUIT TERM

bin/zookeeper-server-start.sh config/zookeeper.properties 2>&1 | sed 's/^/[ZOOKEEPER]/' &
sleep 5
bin/kafka-server-start.sh config/server-0.properties 2>&1 | sed 's/^/[KAFKA-0]/' &
bin/kafka-server-start.sh config/server-1.properties 2>&1 | sed 's/^/[KAFKA-1]/' &
bin/kafka-server-start.sh config/server-2.properties 2>&1 | sed 's/^/[KAFKA-2]/' &
bin/kafka-server-start.sh config/server-3.properties 2>&1 | sed 's/^/[KAFKA-3]/' &
sleep 5
bin/kafka-topics.sh --zookeeper $ZK_SOCK --create --topic packets --partitions 20 --replication-factor 3

echo "Hit ctrl-C to terminate"

wait
