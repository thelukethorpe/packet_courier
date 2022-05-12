# Packet Courier

## A Distributed Network Simulation Tool

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
would translate to something like `python3 server.py Alice 1234 128 127.0.0.2 127.0.0.3 {"Bob": "127.0.0.4"}` at
runtime.

Note that this feature uses environment variable notation as an easy convention, but these are not actually environment
variables at the level of the operating system. A real environment variable such as `${JAVA_HOME}` would not be
translated into something like `/usr/lib/jvm/java-8-oracle` at runtime.

### Emulation Semantics

Packet Courier uses an abstraction of public vs private IP addresses in order to slot the emulated network inbetween
running processes.

### Courier Config File Specification

TODO  
