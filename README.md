#WireFugue

A scalable intrusion detection system based on Akka, Kafka, and Play.

## Synopsis

For this project we shall implement a scalable distributed network monitoring
and intrusion detection system (IDS). By scalable we mean that the system will
scale both to a large number of network sensors and to higher-bandwidth links
by adding computing resources.

Comprehensively monitoring very fast links typically requires specialized
load-balancing hardware. This project will not address those concerns, but
will directly support the ingestion of such demultiplexed flows.

A web dashboard will allow security analysts to display live and historical
network analytics from multiple links, review IDS alerts, adjust capture
filters, and perform forensic analysis on recent data.

Analysts may be allowed to control which events they are interested in from
which interfaces and hosts. They may also request that whole-packet data be
stored for post-hoc retreival and analysis with common tools.

The system is intended to be extensible into a full IDS with add-on
signature-based and anomaly-based detectors capable of responding to network
threats in real time. A major inspiration for this project is the Bro intrusion
detection system which supports a scripting language for identifying various
network phenomena and taking appropriate action.  We envision a similar
domain-specific language (DSL) written in Scala to perform this function in the
future, although it falls outside the scope of work planned for this course.

## Live network analytics

Information displayed will include:
- Bytes sent/received
- Packets sent/received
- Active and recent TCP connections & state
- Recent IP/UDP endpoints
- Request-to-response latency (TCP ACK timing, UDP request/response protocols)
- TCP window size graphs over time


## Featured Technologies
- Akka actors and streams for filtering and event generation at sensor hosts
- Kafka for multiplexing packet and event data from sensors and distributing to analysis and storage hosts
- Vaadin for front-end display and charts
- Scaladin for facilitating Vaadin development in Scala

In the previous course we implemented a parser and analyzer for libpcap capture
files. This work should provide a stepping-stone to live and historical network
analysis.

We chose Apache Kafka as an enabling technology for the following reasons:

- Kafka fits the reactive paradigm (event-driven, scalable, fault-tolerant ...)
- Kafka allows deterministic load-balancing of input streams
  - This is necessary to allow analysis hosts to receive complete TCP streams without complex state tracking logic. 
- Kafka is capable of storing a backlog of events for slower downstream consumers to process
  - Security analysts may wish to examine complete capture data for forensic purposes
- Kafka is capable of feeding real-time data to consumers who can keep up
  - Live network display requires current data
  - Connection tear-down and setting firewall blocks requires low latency to be successful
- Kafka topics provide a natural partitioning for network event streams with varying degrees of detail and from different interfaces
- Kafka enables administrators to specify a retention strategy independent of the application
  - Administrators can set retention policies that jibe with
    - available storage
    - available processing resources
    - privacy and accountability policies

Analysts will have the ability to specify basic filters to apply to network sensors. Analysis hosts can
also tune filters to "shunt" benign data from the analysis system to reduce analysis load.


Sources of network data:

- pcap files
- pipes from tcpdump -w (essentially a "live" pcap file)
- other common appliance monitoring protocols?
- other tools which capture network data?

What sort of events/results might we be interested in?

- Throughput
- Request-response latency
- Endpoints
- Connection-oriented results
  - Stream length (in/out/total)
- Anomalies / invalid header data
- Application-layer information
- Blacklist/whitelist matching: IP addresses, domains, file hashes
