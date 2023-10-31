package thorpe.luke.network.simulation;

import thorpe.luke.log.BufferedFileLogger;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.node.Node;
import thorpe.luke.network.simulation.node.NodeAddress;
import thorpe.luke.network.simulation.node.NodeTopology;
import thorpe.luke.network.simulation.worker.*;
import thorpe.luke.network.socket.Socket;
import thorpe.luke.util.error.ExceptionListener;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class NetworkedPacketCourierSimulation implements Simulation {
  private final PacketCourierSimulation packetCourierSimulation;

  private NetworkedPacketCourierSimulation(PacketCourierSimulation packetCourierSimulation) {
    this.packetCourierSimulation = packetCourierSimulation;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean isComplete() {
    return packetCourierSimulation.isComplete();
  }

  @Override
  public void tick(LocalDateTime now) {
    packetCourierSimulation.tick(now);
  }

    private static void handleException(String name, Logger logger, Exception exception) {
        String stackTrace =
                Arrays.stream(exception.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));
        logger.log(
                String.format(
                        "%s encountered an exception: %s\n%s", name, exception.getMessage(), stackTrace));
    }

    @FunctionalInterface
    private interface WorkerScriptFactory {

        WorkerScript getWorkerScript(
                NodeAddress address,
                NodeTopology nodeTopology,
                int port,
                InetAddress privateIpAddress,
                Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap,
                int datagramBufferSize,
                Map<InetAddress, Socket> privateIpAddressToPublicSocketMap,
                boolean processLoggingEnabled,
                WorkerProcessMonitor workerProcessMonitor,
                Path crashDumpLocation,
                Logger logger);
    }


  public static class Builder {
      private static class State {
          int port;
          InetAddress privateIpAddress;
          Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap;
          int bufferSize;
          Map<InetAddress, Socket> privateIpAddressToPublicSocketMap;
      }
    PacketCourierSimulation.Configuration configuration = PacketCourierSimulation.configuration();
      State state = new State();

      public Builder addNode(String name, WorkerScript workerScript) {
          if (workerScript == null) {
              throw new PacketCourierSimulationConfigurationException("Node script cannot be null.");
          }
      PacketCourierSimulation.WorkerScriptFactory workerScriptFactory =
          (address, topology, crashDumpLocation, logger) -> new WorkerScript() {
              @Override
              public void tick(WorkerManager workerManager) {

              }
          };
          configuration.addNode(name, workerScriptFactory);
          return this;
      }

    public Builder addNode(
            String name, WorkerProcessConfiguration workerProcessConfiguration) {
      if (workerProcessConfiguration == null) {
        throw new PacketCourierSimulationConfigurationException(
                "Node process configuration cannot be null.");
      }
        PacketCourierSimulation.WorkerScriptFactory workerScriptFactory =
              (address,
               topology,
               crashDumpLocation,
               logger) -> {
          // TODO
                WorkerProcess.Factory workerProcessFactory =
                        workerProcessConfiguration.buildFactory(
                                address,
                                topology,
                                state.port,
                                state.privateIpAddress,
                                state.workerAddressToPublicIpMap,
                                state.bufferSize);
                ExceptionListener exceptionListener =
                        exception -> {
                          if (crashDumpLocation == null) {
                            return;
                          }
                          LocalDateTime now = LocalDateTime.now();
                          String crashDumpFileName =
                                  address.getName().replaceAll("\\s+", "-")
                                          + "__"
                                          + PacketCourierSimulation.CRASH_DUMP_DATE_FORMAT.format(now)
                                          + PacketCourierSimulation.CRASH_DUMP_FILE_EXTENSION;
                          File crashDumpFile = crashDumpLocation.resolve(crashDumpFileName).toFile();
                          BufferedFileLogger crashDumpFileLogger;
                          try {
                            crashDumpFileLogger = new BufferedFileLogger(crashDumpFile);
                          } catch (IOException e) {
                            handleException(address.getName(), logger, e);
                            return;
                          }
                          handleException(address.getName(), crashDumpFileLogger, exception);
                          crashDumpFileLogger.flush();
                          crashDumpFileLogger.close();
                        };
                return WorkerPacketForwardingScript.builder()
                        .withPort(state.port)
                        .withPrivateIpAddress(state.privateIpAddress)
                        .withBufferSize(state.bufferSize)
                        .withPrivateIpAddressToPublicSocketMap(state.privateIpAddressToPublicSocketMap)
                        .withExceptionListener(exceptionListener)
                        .build();
              };
        configuration.addNode(name, workerScriptFactory);
      return this;
    }



    public Builder addConnection(
            String sourceName, String destinationName, PacketPipeline.Parameters packetPipelineParameters) {
      configuration.addConnection(sourceName, destinationName, packetPipelineParameters);
      return this;
    }

      public Builder withPort(int port) {
          state.port = port;
          return this;
      }

      public Builder withDatagramBufferSize(int bufferSize) {
          state.bufferSize = bufferSize;
          return this;
      }

    public NetworkedPacketCourierSimulation start() {

    }
  }
}
