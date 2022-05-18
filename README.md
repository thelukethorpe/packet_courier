# Packet Courier

## A Distributed Network Simulation Tool

---

### Introduction

Packet Courier is a Java library that doubles up as both a Java simulation framework and a standalone emulator,
endeavouring to provide those with a curiosity for distributed computer systems with the means to tinker around with
network technologies as though they were running in the real world.

In this way, users can either configure a distributed network simulation programmatically using Java, or they can run
the compiled Jar like an executable in conjunction with a JSON style configuration file to emulate a distributed
network. The benefit of Packet Courier’s emulation component is that it allows users to run arbitrary network code:
their distributed algorithm can leverage any processes that can be run from the command line, whether that be a python
script, a C++ program or a high-level tool such as Kubernetes.

Packet Courier is also able to manipulate packets during transit by creating an intermediate routing layer between nodes
in the network. For example, if Alice sent a packet to Bob, then the Packet Courier framework would silently intercept
this packet and process it as per the given configuration. Users can define network conditions that will govern how
packets behave when travelling from node to node, including properties such as packet latency, loss and corruption.

### Building the Project

Packet Courier uses [Maven](https://maven.apache.org/) for project management. To produce an
executable [Java 8](https://www.oracle.com/java/technologies/java8.html) Jar, simply run `mvn package -DskipTests` in
the root of the repository (where `-DskipTests` is optional). This should then create a directory called `target`,
wherein one should find a file of the form `packet-courier-[version].jar`; this can be used either as a Java library, or
as a standalone executable.

### Running an Emulation

One can execute the compiled Jar to run a distributed network emulation as follows:

`java -jar "target/packet-courier-[version].jar" "path/to/configuration_file.courierconfig"`

Example configuration files can be found in `src/test/resources/thorpe/luke/network/simulation/example` along with their
grammar in `src/main/proto/packet_courier_simulation_configuration.proto`.

A `.courierconfig` file is all that is required to use Packet Courier's emulation feature directly from the command
line. As per the examples, `.courierconfig` files have a JSON structure and allow users to define the parameters of
their emulation, including the topology of their network, which port packets should be sent to and how outcomes should
be logged.

The most conventional way to set up an emulation is to specify:

1) A terminal command for each node in the topology.
2) A set of network conditions for each edge in the topology.

There are a selection of presets that enable users to create large topologies with a minimal configuration, including:

- Star
- Ring
- Linear Daisy Chain
- Fully Connected Mesh

Users can also join these basic topologies together using the joint mesh preset, which allows a star and a ring topology
to enjoy a mutual connection, for example. Conversely, the disjoint mesh preset enables two or more different networks
to execute in parallel with no mutual connections whatsoever.

Packet Courier also offers users the ability to inject emulation meta-data into their commands using a selection of
supported environment variables:

- `NODE_NAME` corresponds to the name that either the configuration file or Packet Courier has assigned to the node
  running the command.
- `PUBLIC_IP` corresponds to this node's public IP address, i.e.: the address that other nodes should send packets to if
  they want Packet Courier to route it back to this node. Similar to a PO Box address.
- `PRIVATE_IP` corresponds to the node's private IP address, i.e.: the address that this node should listen on to
  receive packets. Similar to a house address.
- `PORT` corresponds to the port that nodes have been configured to listen on.
- `DATAGRAM_BUFFER_SIZE` corresponds to the buffer size of the UDP sockets that Packet Courier is listening on.
- `NEIGHBOUR_IPS` corresponds to the names and public IP addresses of neighbouring nodes.
- `FLOOP_IPS_BY_DISTANCE=[distance]` generates the names and public IP addresses of a basic flood search with search
  radius`distance`.
- `TOPOLOGY_IPS` corresponds to the names and public IP addresses of the entire topology, including nodes that this node
  isn't adjacent to and therefore cannot communicate with directly.

For example, if Alice and Bob were configured to be bidirectionally connected, listening on port 1234 with a datagram
buffer size of 128, then Alice's
command `python3 server.py ${NODE_NAME} ${PORT} ${DATAGRAM_BUFFER_SIZE} ${PRIVATE_IP} ${PUBLIC_IP} ${NEIGHBOUR_IPS}`
would translate to something like `python3 server.py Alice 1234 128 127.0.0.3 127.0.0.4 {"Bob": "127.0.0.6"}` at
runtime.

Note that this feature uses environment variable notation as an easy convention, but these are not actually environment
variables at the level of the operating system. A real environment variable such as `${JAVA_HOME}` would not be
translated into something like `/usr/lib/jvm/java-8-oracle` at runtime.

### Emulation Semantics

Packet Courier uses an abstraction of public vs private IP addresses in order to slot the emulated network inbetween
running processes. Each node is assigned a _private_ IP address which corresponds to the address it should be sending
and receiving packets on. Whilst nodes could just communicate purely using these IP addresses, they would simply be
passing messages using UDP as a protocol and the operating system kernel as a wire; there would be no scope for
manipulating the transmission of packets in a way that mimics a particular type of internet connection.

To solve this problem, Packet Courier introduces the notion of a _public_ IP address, which allows Packet Courier to
intercept packets and modify them as per the user configuration. By way of analogy, one could think of a private IP as a
home address and a public IP as a [PO Box](https://en.wikipedia.org/wiki/Post_office_box). Suppose someone doesn't want
mail to be sent directly to their home for whatever reason; in this instance they can open a PO Box, whereby they
collect parcels and letters that they know are intended for them in an environment where they can be screened and
processed.

For example, consider bidirectionally connected nodes Alice and Bob. Alice is told at runtime that her private IP
is `127.0.0.3`, her public IP is `127.0.0.4` and she is neighboured with Bob who has a public IP of `127.0.0.6`. When
Alice wants to send a packet to Bob, she should send it from her _private_ UDP socket with address `127.0.0.3` to the
destination of `127.0.0.6`. Alice will in fact be sending her packet to the Packet Courier routing layer, which will
determine that this is Alice attempting to send a packet to Bob, in turn applying any network conditions such as
latency, loss and corruption to the packet, before forwarding it on to Bob's _private_ IP, ensuring that the source is
listed as Alice's _public_ IP.

![Public vs Private IP Diagram](/doc/readme/emulation_semantics_diagram.jpg)

One potential issue with this approach is that nodes could (accidentally or otherwise) simply pass on their private IP
address and have other nodes bypass the emulator entirely. There is of course nothing that can realistically be done to
stop this, in the same way that there is nothing stopping users from hard coding IP addresses into their protocols which
would have a similar effect. Ideally Packet Courier would manipulate packets at the level of the kernel, bypassing the
need for this architecture in the first place, however this just isn't very feasible in Java, let alone in a way that is
portable, containerised and platform-agnostic. Thus, users are encouraged to bear these limitations in mind when using
the tool.

---

### Courier Config File Specification

`topology :: Topology` ~ the arrangement of nodes and edges in the emulated network.

`wallClockEnabled :: boolean` ~ if set to `true`, then any time-based semantics such as latency use the wall clock
(better for _emulations_), as opposed to a virtual clock that ticks with each round of CPU scheduling (better for
_simulations_).

`processLoggingEnabled :: boolean` ~ if set to `true`, then the console output of each process will be logged upon the
process exiting with code zero.

`port :: int32` ~ _optional field_: the port that Packet Courier should listen on.

`datagramBufferSize :: int32` ~ _optional field_: the buffer size of the UDP sockets that Packet Courier is listening
on.

`loggers :: [Logger]` ~ the channels on which Packet Courier should log activity.

`simulationName :: string` ~ _optional field_: the name associated with this particular configuration; used in meta
logging, etc.

`seed :: int32` ~ _optional field_: the seed used by the Packet Courier's random number generators; useful for testing.

`debug :: Debug` ~ specifies options which are useful when debugging, such as configuring a crash dump location, meta
logging and process monitoring.

---

#### Debug

`processMonitorEnabled :: boolean` ~ if set to `true`, then the status of each running process will be periodically
meta-logged.

`processMonitorCheckupInterval :: Duration` ~ _optional field_: the interval between process monitor checkups.

`tickDurationSampleSize :: int32` ~ _optional field_: a Packet Courier **tick** corresponds to **one round of packet
processing**. For example, if Alice sends a packet to Bob via Packet Courier, then the packet will arrive at one of
Packet Courier's UDP sockets and wait to be processed. When Packet Courier next ticks over, the packet will be pulled
from its residing socket and pushed one stage through the packet pipeline associated with Alice's connection with Bob.
If this pipeline consisted of a latency and a corruption component, then it would take 2 ticks for the packet to be sent
to Bob.

As part of the debugging process, the time spent processing each Packet Courier tick is measured, namely by taking an
average over a number of samples. This option specifies the number of samples taken for each computation of the average
tick duration.

This is an important statistic to consider when building an emulation; if a tick is taking over a millisecond on
average, then milliseconds are unlikely to be a suitable unit for latency, for instance.

`crashDumpLocation :: string` ~ _optional field_: where crash dump files will be written in the event that a process
exits with a non-zero code.

`metaLoggers :: [Logger]` ~ the channels on which Packet Courier should log meta activity.

---

#### Duration

`length :: int64` ~ the scalar value associated with the duration, i.e.: there are **60** minutes in an hour.

`timeUnit :: TimeUnit` ~ the unit associated with the duration, i.e.: there are 60 **minutes** in an hour.

---

#### Topology

Constitutes exactly one of the following:

- `custom :: CustomTopology`

- `star :: StarTopology`

- `ring :: RingTopology`

- `linearDaisyChain :: LinearDaisyTopology`

- `fullyConnectedMesh :: FullyConnectedMeshTopology`

- `jointMesh :: JointMeshTopology`

- `disjointMesh :: DisjointMeshTopology`

A _joint mesh_ is a topology that consists of multiple sub-topologies connected together, whereby each sub-topology
joins the mesh at a specific "joining" node. By way of analogy, if three companies were going to merge, then at least
one spokesperson from each company would be needed to initiate some kind of negotiation. In this way, a joint mesh
consists of multiple otherwise separate topologies that boast mutual connections at specific points of entry.

A _disjoint mesh_, on the other hand, is a topology that consists of multiple sub-topologies that are totally
disconnected from one another.

---

#### CustomTopology

A custom topology allows users to simply enter each and every node and edge by hand.

`commandNodes :: [CommandNode]` ~ the nodes of the topology.

`connections :: [Connection]` ~ the edges of the topology.

`joiningNodeName :: string` ~ _optional field_: the name of the node which has been nominated to serve as a point of
entry in a joint mesh. If this field is left empty, then this topology will remain isolated even if it is used in the
context of a joint mesh.

---

#### StarTopology

A topology that consists of a central server and clients which connect to it. Node names are procedurally generated.
The "joining node" is always the server.

`serverScript :: Script` ~ the script which gets run on the server node.

`clientScript :: Script` ~ the script which gets run on the client node(s).

`unidirectional :: boolean` ~ if set to `true`, then clients will be able to send packets to the server, but not the
other way around.

`size :: int32` ~ the number of clients in the topology. Even if set to zero, the server will still persist.

`networkConditions :: [NetworkCondition]` ~ the server-client network conditions.

---

#### RingTopology

A topology that consists of nodes connected in a linear, cyclic chain. Node names are procedurally generated. The
"joining node" is chosen at random due to the symmetric nature of the topology.

`script :: Script` ~ the script which gets run on each node.

`unidrectional :: boolean` ~ if set to `true`, then nodes will only be able to pass packets in one direction around the
ring.

`size :: int32` ~ the number of nodes in the ring.

`networkConditions :: [NetworkCondition]` ~ the network conditions between nodes in the ring.

---

#### LinearDaisyChainTopology

A topology that consists of nodes connected in a linear, non-cyclic chain, i.e.: a ring with a link broken. Node names
are procedurally generated. The "joining node" is the "last" node in the chain: in a bidirectional chain, this could be
either end; in a unidirectional chain, this is the _sink node_, i.e.: where all packets will end up if the network is
flooded.

`script :: Script` ~ the script which gets run on each node.

`unidrectional :: boolean` ~ if set to `true`, then nodes will only be able to pass packets in one direction down the
chain.

`size :: int32` ~ the number of nodes in the chain.

`networkConditions :: [NetworkCondition]` ~ the network conditions between nodes in the chain.

---

#### FullyConnectedMeshTopology

A topology where every node is connected to every other node. Node names are procedurally generated. The "joining node"
is chosen at random due to the symmetric nature of the topology.

`script :: Script` ~ the script which gets run on each node.

`size :: int32` ~ the number of nodes in the mesh.

`networkConditions :: [NetworkCondition]` ~ the network conditions between nodes in the mesh.

---

#### JointMeshTopology

`jointTopologies :: [Topology]` ~ the topologies which are to be joint-up into a mesh.

`networkConditions :: [NetworkCondition]` ~ the network conditions used in the connections between topologies.

`joiningNodeName :: string` ~ _optional field_: the name of the node which has been nominated to serve as a point of
entry in a _recursive_ joint mesh, i.e.: a joint mesh of joint meshes. If this field is left empty, then this topology
will remain isolated even if it is used in the context of a joint mesh.

---

#### DisjointMeshTopology

`disjointTopologies :: [Topology]` ~ the topologies which are to coexist in a disjoint mesh.

`joiningNodeName :: string` ~ _optional field_: the name of the node which has been nominated to serve as a point of
entry in a _recursive_ joint mesh, i.e.: a joint mesh of (dis)joint meshes. If this field is left empty, then this
topology will remain isolated even if it is used in the context of a joint mesh.

---

#### Script

`command :: string` ~ the command used to start the process associated with the node running this script.

`timeout :: Duration` ~ _optional field_: the time-to-live of the process started by `command`.

---

#### CommandNodeProto

`name :: string` ~ the name given to this node; must be unique.

`script :: Script` ~ the script run by this node on startup.

---

#### Connection Proto

`sourceNodeName :: string` ~ the name of the source node.

`destinationNodeName :: string` ~ the name of the destination node.

`networkConditions :: [NetworkCondition]` ~ the network conditions associated with this connection.

---

#### NetworkCondition

Constitutes exactly one of the following:

- `packetLimitParameters :: PacketLimitParameters`

- `packetThrottleParameters :: PacketThrottleParameters`

- `packetCorruptionParameters :: PacketCorruptionParameters`

- `packetDropParameters :: PacketDropParameters`

- `packetDuplicationParameters :: PacketDuplicationParameters`

- `packetLatencyParameters :: PacketLatencyParameters`

- `eventPipelineParameters :: EventPipelineParameters`

Network conditions that encode for a packet processing pipeline are sensitive to the order in which they are presented.
For example, if a list of network conditions `[NetworkCondition]` is specified as:

```
"networkConditions" : [
  {
    "packetLatencyParameters": {
      ...
    }
  },
  {
    "packetDuplicationParameters": {
      ...
    }
  }
]
```

then this would correspond to a packet processing pipeline that delayed its packets before it duplicated them. This is
likely to be suboptimal, however, since packet duplication does no reordering, but latency (usually) does. In addition,
this will mean that duplicated packets will all boast the exact same latency. In most cases, this would be seen as
rather strange network behaviour. Thus, if a user wishes for all of their packets to be reordered and subject to a
bespoke latency, then duplication would be best put before latency.

---

#### PacketLimitParameters

Packet limiting involves only allowing a certain number of packets across a connection over a particular period of time,
whereby excess packets are simply dropped.

`packetLimitRate :: int32` ~ the maximum number of packets accepted per unit time.

`timeUnit :: TimeUnit` ~ the unit of time associated with `packetLimitRate`.

---

#### PacketThrottleParameters

Packet throttling involves controlling the number of bytes that can be transmitted across a connection over a particular
period of time, whereby no packets are dropped; instead the bitrate of the connection is controlled. In this way, packet
throttling does risk becoming a black hole with respect to memory, particularly in cases where a high-throughput
connection is being heavily throttled over a long period of time. As such, packet throttling should mainly be used to
smooth out connections that are prone to burst behaviours, rather than as a cheap and dirty bitrate control mechanism.

`byteThrottleRate :: int32` ~ the maximum number of bytes transmitted per unit time.

`timeUnit :: TimeUnit` ~ the unit of time associated with `byteThrottleRate`.

---

#### PacketCorruptionParameters

`corruptionProbability :: double` ~ the probability that a packet will be corrupted, i.e.: have a random bit flipped.

---

#### PacketDropParameters

`dropProbability :: double` ~ the probability that a packet will be dropped.

---

#### PacketDuplicationParameters

`meanDuplications :: double` ~ the mean number of times a packet will be duplicated.

---

#### PacketLatencyParameters

Constitutes exactly one of the following:

- `exponential :: ExponentialDistributionParameters`

- `normal :: NormalDistributionParameters`

- `uniform :: UniformRealDistributionParameters`

Packet latency involves sampling from one of the above distributions and delaying the packet by the result with respect
to the provided `timeUnit`:

`timeUnit :: TimeUnit` ~ the unit of time associated with the delay added to packets.

---

#### EventPipelineParameters

An event pipeline introduces event-based semantics into Packet Courier. Event-based semantics allow network conditions
to vary dramatically over time, namely in a stateful way. This is very useful for emulating mobile connections, for
example, where a client might move under a bridge or behind a tree and experience a momentary dip in quality of service.

When no events are currently in progress, an event pipeline will resort back to a default set of network conditions.
However, when the event scheduler invokes an event, the network conditions associated with that event will take over for
the duration of the event. If multiple events are taking place at the same time, the event with the highest precedence
will have priority in its network conditions. Precedence is determined by the order that events are specified in the
configuration file, i.e.:

```
"eventPipelineParameters" : {
  "timeUnit": "MILLI_SECONDS",
  "defaultNetworkConditions": [
    ...
  ],
  "networkEvents": [
    {
      // Event with precedence 0.
      ...
    },
    {
      // Event with precedence 1.
      ...
    },
    {
      // Event with precedence 2.
      ...
    }
  ]
}
```

`timeUnit :: TimeUnit` ~ the unit of time associated with the interval and duration parameters specified as part of this
event pipeline.

`defaultNetworkConditions :: [NetworkCondition]` ~ the network conditions which are active when no event is taking
place.

`networkEvents :: [NetworkEvent]` ~ the events that could be invoked as part of the event pipeline.

---

#### NetworkEvent

`meanInterval :: double` ~ the average time between invocations of this event.

`meanDuration :: double` ~ the average amount of time this event lasts for.

`networkConditions :: [NetworkCondition]` ~ the network conditions that this event will instantiate whilst it is active
and has the highest precedence of active events.

---

#### ExponentialDistributionParameters

`lambda :: double`

---

#### NormalDistributionParameters

`mean :: double`

`standardDeviation :: double`

---

#### UniformRealDistributionParameters

`minimum :: double`

`maximum :: double`

---

#### TimeUnit

An _enum_ that constitutes exactly one of the following:

- `NANO_SECONDS`

- `MICRO_SECONDS`

- `MILLI_SECONDS`

- `SECONDS`

- `MINUTES`

- `HOURS`

- `DAYS`

- `WEEKS`

- `MONTHS`

- `YEARS`

- `DECADES`

- `CENTURIES`

- `MILLENNIA`

- `ERAS`

- `FOREVER`
