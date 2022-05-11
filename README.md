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


#### Courier Config File Specification

TODO  
