# Wirefugue component capabilities

## sensor
- detects raw capture data from: pcap file [DONE], pcap pipe [DONE]
- produces kafka messages
- partitions the kafka messages in a sensible and balanced manner
- partitions such that whole tcp streams can be read from a single partition
- [WISHLIST] defragments ip packets (necessary to determine kafka partition because port info 
only in first fragment)
- [WISHLIST]

## analysis "agents"
- consumes messages from various kafka streams in real time
- produces new streams, including higher-level event streams [what events?]
- detects "strange" standalone packets (bad header info, weird flags)
- [WISHLIST] defragments ip
- reassembles tcp
- publishes individual tcp streams as streams in their own right
- request/response (latency) analysis
- detects site-wide scans by analyzing events from many different streams
- [WISHLIST] decodes http entities
- [WISHLIST] detects content within http entities
- [WISHLIST] uses a machine-learning model to infer the threat level for various types of traffic
- [WISHLIST] allows analysts to train the machine-learning model to respond better to
  the network operators' policies and preferences.

## control backend
- run this to start the world
- launches services on cloud platform(s) (aws, heroku, docker)
    - ensures that sensors are deployed
    - starts frontend node swarm
    - starts kafka cluster
    - supervises all components, restarting or alerting admins if components fail
- responds to requests from frontend
- consumes highest-level streams from kafka
- pushes data to frontend

## frontend
- observe real time network behavior: traffic rates, classification
- graphing with [d3.js](https://github.com/spaced/scala-js-d3) via scala?
- event viewer (log display)
- upload packet trace files
- show availability of historical data
- [WISHLIST] set retention policies
- [WISHLIST] seats for network analysts and control specialists (authenticated logins w/ roles, capabilities)
- [WISHLIST] manual event sender: firewall block, unblock, connection teardown, admin notification
- [WISHLIST] injection

# Development & Deployment Strategies

## testing

- unit tests with sbt run (scalatest, scalacheck, akka testkit)
- Jenkins + Docker
- [WISHLIST] circleci
- Ostinato for traffic generation

## development

- github / git
- markdown for README

## deployment

- on laptop for demonstration
- docker
- aws
