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