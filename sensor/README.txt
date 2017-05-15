Ian Robertson
Week 8 Homework - Streams

University of Washington Professional & Continuing Education
Concurrent Programming with Akka
Instructor: Jerry Kuch

Objective
=========

The purpose of this assignment is to experiment with Akka
streams. We chose to use Akka streams to analyze network
capture files of the format produced by the libpcap network
capture library.

Capture files may be very large, and so cannot necessarily
be read into memory all at once. We saw this as a prime
opportunity to develop a system whereby such files can be
processed incrementally to produce aggregate data, such as
the number of packets observed, the number of bytes observed,
and a list of endpoints of IP interactions.

Implementation
==============

The centerpiece of this apparatus is a custom stateful GraphStage which
accepts streams of ByteString from an Akka FileIO source and
produces a stream of PcapMessage objects. A PcapHeader representing
global file-level information is
emitted first, followed by zero or more Packets, representing
each subsequent record in the file, corresponding to one layer-2
PDU (protocol data unit) apiece.

The PcapHeader contains information required to interpret
each packet, namely a time zone offset for the timestamp,
and a LinkType identifier, which specifies the header format of
each Packet object's payload. Thus there is a second stage,
awkwardly named "PacketContextualizer", which remembers the
last PcapHeader and uses this data to convert Packet objects
to UPacket objects, a self-contained type with a UTC
timestamp and the LinkType information included.

In order to provide an efficient implementation for saving
capture files, libpcap may write headers in either little-
or big-endian formats. A four-byte magic number at the beginning
of the file helps identify which sort of architecture wrote
the file. Thus provisions had to be made for reading these headers
in either format. The ByteStringOps class implements extensions
to subclasses of Scala's IndexedSeq[Byte], using Scala's implicit
conversion machinery, to implement reading multi-byte integers
of different widths in big- or little-endian formats. Packet
payload data is always written in network byte order, which is
in practice always big-endian.

To limit the scope of this work, we chose only to implement
parsing for Ethernet link-layer headers and IPv4 headers.

Once packets have been extracted and parsed, we can use simple
stream transformations, filters, and folds to obtain useful
data. Some examples are provided in
src/test/scala/edu/uw/at/iroberts/PcapFileParserSpec,
the most complex of which yields a Set of all IP endpoints
observed in a single file.

There is also a standalone Java application, PcapFileDemo,
which reads the http.cap file and produces a human-readable
interpretation of the packets similar to the output of the GNU tcpdump
tool.

Future Directions
=================

While the current work concerns itself only with reading
files, it should be readily adaptable to handling live
capture streams, perhaps from a Unix or Linux fifo that
has been configured to receive data from a tool like tcpdump.
A handful
of Java libraries also exist to interact with the native
libpcap library, which could be used to obtain a live stream.

This tool could also be extended to write back to the libpcap
format, which would be useful for transforming byte order in
the headers, filtering out certain packet types, converting
one link-layer header type to another, truncating
stored data, and so on.

By writing live result data to an injector, real-time packet
mangling and software-defined networking could be implemented,
although we expect significant performance hurdles to achieve
usefulness here. Still, there may be applications for network
simulation where this is not a concern.

Additionally, very few protocols and link-layer types are supported.
It is a massive project to incorporate even the most ubiquitous ones.
TCP header parsing isn't even included yet, and only the most
useful fields of the IP header are available to clients.

References
==========

Libpcap File Format
    https://wiki.wireshark.org/Development/LibpcapFileFormat
Link-layer Header Types
    http://www.tcpdump.org/linktypes.html
Sample Captures
    https://wiki.wireshark.org/SampleCaptures
Wikipedia: Ethernet Frame
    https://en.wikipedia.org/wiki/Ethernet_frame
Wikipedia: IPv4
    https://en.wikipedia.org/wiki/IPv4



