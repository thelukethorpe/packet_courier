package thorpe.luke.network.simulation;

import thorpe.luke.log.BufferedFileLogger;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.node.Node;
import thorpe.luke.network.simulation.worker.*;
import thorpe.luke.network.socket.Socket;
import thorpe.luke.network.socket.SocketFactory;
import thorpe.luke.network.socket.UniqueLoopbackIpv4AddressGenerator;
import thorpe.luke.util.error.ExceptionListener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

public class NetworkedPacketCourierSimulation implements Simulation {
  private final PacketCourierSimulation baseSimulation;

  private NetworkedPacketCourierSimulation(PacketCourierSimulation baseSimulation) {
    this.baseSimulation = baseSimulation;
  }

  public static Builder builderWithSocketFactory(SocketFactory socketFactory) {
    return new Builder(socketFactory);
  }

  @Override
  public boolean isComplete() {
    return baseSimulation.isComplete();
  }

  @Override
  public void tick(LocalDateTime now) {
    baseSimulation.tick(now);
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


  public static class Builder {
      private static class State {

          int port;
          Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap;
          int bufferSize;
          Map<InetAddress, Socket> privateIpAddressToPublicSocketMap;
          Logger logger;
      }

      private final State state = new State();
      private final PacketCourierSimulation.Configuration configuration = PacketCourierSimulation.configuration();
    private final Collection<WorkerProcessConfiguration> workerProcessConfigurations =
        new LinkedList<>();
      private final UniqueLoopbackIpv4AddressGenerator uniqueLoopbackIpv4AddressGenerator =
              new UniqueLoopbackIpv4AddressGenerator();
      private final SocketFactory socketFactory;

      public Builder(SocketFactory socketFactory) {
          this.socketFactory = socketFactory;
      }

      private InetAddress generateUniqueIpAddress() {
          try {
              InetAddress ipAddress = uniqueLoopbackIpv4AddressGenerator.generateUniqueIpv4Address();
              if (ipAddress == null) {
                  throw new PacketCourierSimulationConfigurationException(
                          "The Kernel has run out of fresh ip addresses.");
              }
              return ipAddress;
          } catch (UnknownHostException e) {
              throw new PacketCourierSimulationConfigurationException(e);
          }
      }

    public Builder addNode(
            String name, WorkerProcessConfiguration workerProcessConfiguration) {
      if (workerProcessConfiguration == null) {
        throw new PacketCourierSimulationConfigurationException(
                "Node process configuration cannot be null.");
      }
//        workerProcessConfigurations.add(workerProcessConfiguration);
//
        InetAddress privateIpAddress = generateUniqueIpAddress();
        InetAddress publicIpAddress = generateUniqueIpAddress();
        PacketCourierSimulation.WorkerScriptFactory workerScriptFactory =
              (address,
               topology,
               crashDumpLocation,
               logger) -> {

                WorkerProcess.Factory workerProcessFactory =
                        workerProcessConfiguration.buildFactory(
                                address,
                                topology,
                                state.port,
                                privateIpAddress,
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
                  WorkerPacketForwardingScript packetForwardingScript = WorkerPacketForwardingScript.builder()
                        .withPort(state.port)
                        .withPrivateIpAddress(privateIpAddress)
                        .withBufferSize(state.bufferSize)
                        .withPrivateIpAddressToPublicSocketMap(state.privateIpAddressToPublicSocketMap)
                        .withExceptionListener(exceptionListener)
                        .build();
                  WorkerProcess workerProcess = workerProcessFactory.start();
                                return new WorkerScript() {
              @Override
              public void tick(WorkerManager workerManager) {
                  if (!workerProcess.isAlive()) {
                      workerProcess.logProcessOutput(state.logger, Integer.MAX_VALUE);
                      workerProcess.waitFor();
                      workerManager.destroy();
                      return;
                  }
                  packetForwardingScript.tick(workerManager);
                  // TODO
                  workerProcess.logProcessOutput(state.logger, 100);
              }};
              };
        Node node = configuration.addNode(name, workerScriptFactory);
        state.privateIpAddressToPublicSocketMap.put(privateIpAddress, socketFactory.getSocket(publicIpAddress));
        state.workerAddressToPublicIpMap.put(node.getAddress().asRootWorkerAddress(), publicIpAddress);
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

      public Builder withBufferSize(int bufferSize) {
          state.bufferSize = bufferSize;
          return this;
      }

    public NetworkedPacketCourierSimulation start() {

    }
  }
}
