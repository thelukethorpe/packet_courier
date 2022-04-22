package thorpe.luke.network.simulation;

import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import thorpe.luke.log.BufferedFileLogger;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.NetworkEvent;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.node.DefaultNodeInfo;
import thorpe.luke.network.simulation.node.NodeInfoGenerator;
import thorpe.luke.network.simulation.worker.WorkerProcessConfiguration;
import thorpe.luke.network.simulation.worker.WorkerScript;

public class PacketCourierSimulationConfigurationProtoParser<NodeInfo> {

  private final PacketCourierSimulation.Configuration<NodeInfo> configuration;
  private final Map<String, WorkerScript<NodeInfo>> nodeNameToWorkerScriptWorkList;
  private final Random random;

  private PacketCourierSimulationConfigurationProtoParser(
      PacketCourierSimulation.Configuration<NodeInfo> configuration,
      Map<String, WorkerScript<NodeInfo>> nodeNameToWorkerScriptWorkList,
      Random random) {
    this.configuration = configuration;
    this.nodeNameToWorkerScriptWorkList = nodeNameToWorkerScriptWorkList;
    this.random = random;
  }

  public static PacketCourierSimulation.Configuration<DefaultNodeInfo> parse(File protobufFile) {
    return parse(protobufFile, DefaultNodeInfo.generator(), new HashMap<>());
  }

  public static <NodeInfo> PacketCourierSimulation.Configuration<NodeInfo> parse(
      File protobufFile,
      NodeInfoGenerator<NodeInfo> nodeInfoGenerator,
      Map<String, WorkerScript<NodeInfo>> nodeNameToWorkerScriptMap) {
    PacketCourierSimulationConfigurationProto configurationProto;
    try {
      configurationProto = readProtobufFile(protobufFile);
    } catch (IOException e) {
      throw new PacketCourierSimulationConfigurationProtoParserException(e);
    }
    PacketCourierSimulation.Configuration<NodeInfo> configuration =
        PacketCourierSimulation.configuration(nodeInfoGenerator);
    Random random =
        configurationProto.hasSeed() ? new Random(configurationProto.getSeed()) : new Random();
    PacketCourierSimulationConfigurationProtoParser<NodeInfo> parser =
        new PacketCourierSimulationConfigurationProtoParser<>(
            configuration, new HashMap<>(nodeNameToWorkerScriptMap), random);
    return parser.parseConfiguration(configurationProto);
  }

  private static PacketCourierSimulationConfigurationProto readProtobufFile(File protobufFile)
      throws IOException {
    PacketCourierSimulationConfigurationProto.Builder protoBuilder =
        PacketCourierSimulationConfigurationProto.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(new FileReader(protobufFile), protoBuilder);
    return protoBuilder.build();
  }

  private PacketCourierSimulation.Configuration<NodeInfo> parseConfiguration(
      PacketCourierSimulationConfigurationProto configurationProto) {
    for (CommandNodeProto commandNodeProto : configurationProto.getCommandsNodesList()) {
      String name = commandNodeProto.getName();
      String command = commandNodeProto.getCommand();
      configuration.addNode(name, WorkerProcessConfiguration.fromCommand(command));
    }

    if (configurationProto.getWallClockEnabled()) {
      configuration.usingWallClock();
    }
    if (configurationProto.getProcessLoggingEnabled()) {
      configuration.withProcessLoggingEnabled();
    }
    if (configurationProto.hasCrashDumpLocation()) {
      configuration.withCrashDumpLocation(Paths.get(configurationProto.getCrashDumpLocation()));
    }
    if (configurationProto.hasPort()) {
      configuration.withPort(configurationProto.getPort());
    }
    if (configurationProto.hasDatagramBufferSize()) {
      configuration.withDatagramBufferSize(configurationProto.getDatagramBufferSize());
    }

    configurationProto
        .getLoggersList()
        .forEach(loggerProto -> configuration.addLogger(parseLogger(loggerProto)));

    for (ConnectionProto connectionProto : configurationProto.getConnectionsList()) {
      String sourceNodeName = connectionProto.getSourceNodeName();
      WorkerScript<NodeInfo> sourceNodeWorkerScript =
          nodeNameToWorkerScriptWorkList.get(sourceNodeName);
      if (sourceNodeWorkerScript != null) {
        configuration.addNode(sourceNodeName, sourceNodeWorkerScript);
        nodeNameToWorkerScriptWorkList.remove(sourceNodeName);
      }
      String destinationNodeName = connectionProto.getDestinationNodeName();
      WorkerScript<NodeInfo> destinationNodeWorkerScript =
          nodeNameToWorkerScriptWorkList.get(destinationNodeName);
      if (destinationNodeWorkerScript != null) {
        configuration.addNode(destinationNodeName, destinationNodeWorkerScript);
        nodeNameToWorkerScriptWorkList.remove(destinationNodeName);
      }
      PacketPipeline.Parameters packetPipelineParameters =
          PacketPipeline.parameters(
              connectionProto
                  .getNetworkConditionsList()
                  .stream()
                  .map(this::parseNetworkCondition)
                  .collect(Collectors.toList()));
      configuration.addConnection(sourceNodeName, destinationNodeName, packetPipelineParameters);
    }
    return configuration;
  }

  private static Logger parseLogger(LoggerProto loggerProto) {
    switch (loggerProto.getLoggerParametersCase()) {
      case CONSOLE:
        return parseConsoleLogger(loggerProto.getConsole());
      case FILE:
        return parseFileLogger(loggerProto.getFile());
    }
    throw new PacketCourierSimulationConfigurationProtoParserException(
        "Logger Proto is missing parameters.");
  }

  private static ConsoleLogger parseConsoleLogger(ConsoleLoggerProto consoleLoggerProto) {
    switch (consoleLoggerProto) {
      case STDOUT:
        return ConsoleLogger.out();
      case STDERR:
        return ConsoleLogger.err();
    }
    throw new PacketCourierSimulationConfigurationProtoParserException(
        "Console Logger Proto not recognized.");
  }

  private static BufferedFileLogger parseFileLogger(FileLoggerProto fileLoggerProto) {
    try {
      return new BufferedFileLogger(Paths.get(fileLoggerProto.getPath()).toFile());
    } catch (IOException e) {
      throw new PacketCourierSimulationConfigurationProtoParserException(e);
    }
  }

  private NetworkCondition parseNetworkCondition(NetworkConditionProto networkConditionProto) {
    switch (networkConditionProto.getParametersCase()) {
      case PACKETLIMITPARAMETERS:
        return parsePacketLimitParameters(networkConditionProto.getPacketLimitParameters());
      case PACKETTHROTTLEPARAMETERS:
        return parsePacketThrottleParameters(networkConditionProto.getPacketThrottleParameters());
      case PACKETCORRUPTIONPARAMETERS:
        return parsePacketCorruptionParameters(
            networkConditionProto.getPacketCorruptionParameters());
      case PACKETDROPPARAMETERS:
        return parsePacketDropParameters(networkConditionProto.getPacketDropParameters());
      case PACKETDUPLICATIONPARAMETERS:
        return parsePacketDuplicationParameters(
            networkConditionProto.getPacketDuplicationParameters());
      case PACKETLATENCYPARAMETERS:
        return parsePacketLatencyParameters(networkConditionProto.getPacketLatencyParameters());
      case EVENTPIPELINEPARAMETERS:
        return parseEventPipelineParameters(networkConditionProto.getEventPipelineParameters());
    }
    throw new PacketCourierSimulationConfigurationProtoParserException(
        "Network Condition Proto is missing parameters.");
  }

  private NetworkCondition parsePacketLimitParameters(
      PacketLimitParametersProto packetLimitParametersProto) {
    return NetworkCondition.packetLimit(packetLimitParametersProto.getPacketLimit());
  }

  private NetworkCondition parsePacketThrottleParameters(
      PacketThrottleParametersProto packetThrottleParametersProto) {
    return NetworkCondition.packetThrottle(
        packetThrottleParametersProto.getByteThrottleRate(),
        parseTimeUnit(packetThrottleParametersProto.getTimeUnit()));
  }

  private NetworkCondition parsePacketCorruptionParameters(
      PacketCorruptionParametersProto packetCorruptionParametersProto) {
    return NetworkCondition.uniformPacketCorruption(
        packetCorruptionParametersProto.getCorruptionProbability(), random);
  }

  private NetworkCondition parsePacketDropParameters(
      PacketDropParametersProto packetDropParametersProto) {
    return NetworkCondition.uniformPacketDrop(
        packetDropParametersProto.getDropProbability(), random);
  }

  private NetworkCondition parsePacketDuplicationParameters(
      PacketDuplicationParametersProto packetDuplicationsParametersProto) {
    return NetworkCondition.poissonPacketDuplication(
        packetDuplicationsParametersProto.getMeanDuplications(), random);
  }

  private NetworkCondition parsePacketLatencyParameters(
      PacketLatencyParametersProto packetLatencyParametersProto) {
    ChronoUnit timeUnit = parseTimeUnit(packetLatencyParametersProto.getTimeUnit());
    switch (packetLatencyParametersProto.getLatencyDistributionParametersCase()) {
      case EXPONENTIAL:
        ExponentialDistributionParametersProto exponentialDistributionParametersProto =
            packetLatencyParametersProto.getExponential();
        return NetworkCondition.exponentialPacketLatency(
            exponentialDistributionParametersProto.getLambda(), timeUnit, random);
      case NORMAL:
        NormalDistributionParametersProto normalDistributionParametersProto =
            packetLatencyParametersProto.getNormal();
        return NetworkCondition.normalPacketLatency(
            normalDistributionParametersProto.getMean(),
            normalDistributionParametersProto.getStandardDeviation(),
            timeUnit,
            random);
      case UNIFORM:
        UniformRealDistributionParametersProto uniformRealDistributionParametersProto =
            packetLatencyParametersProto.getUniform();
        return NetworkCondition.uniformPacketLatency(
            uniformRealDistributionParametersProto.getMinimum(),
            uniformRealDistributionParametersProto.getMaximum(),
            timeUnit,
            random);
    }
    throw new PacketCourierSimulationConfigurationProtoParserException(
        "Packet Latency Proto is missing distribution.");
  }

  private NetworkCondition parseEventPipelineParameters(
      EventPipelineParametersProto eventPipelineParametersProto) {
    ChronoUnit timeUnit = parseTimeUnit(eventPipelineParametersProto.getTimeUnit());
    PacketPipeline.Parameters defaultPacketPipelineParameters =
        PacketPipeline.parameters(
            eventPipelineParametersProto
                .getDefaultNetworkConditionsList()
                .stream()
                .map(this::parseNetworkCondition)
                .collect(Collectors.toList()));
    List<NetworkEvent> networkEvents =
        eventPipelineParametersProto
            .getNetworkEventsList()
            .stream()
            .map(this::parseNetworkEvent)
            .collect(Collectors.toList());
    return NetworkCondition.eventPipeline(
        timeUnit, random, defaultPacketPipelineParameters, networkEvents);
  }

  private NetworkEvent parseNetworkEvent(NetworkEventProto networkEventProto) {
    return NetworkEvent.builder()
        .withMeanInterval(networkEventProto.getMeanInterval())
        .withMeanDuration(networkEventProto.getMeanDuration())
        .buildWithNetworkConditions(
            networkEventProto
                .getNetworkConditionsList()
                .stream()
                .map(this::parseNetworkCondition)
                .collect(Collectors.toList()));
  }

  private static ChronoUnit parseTimeUnit(TimeUnitProto timeUnitProto) {
    switch (timeUnitProto) {
      case NANO_SECONDS:
        return ChronoUnit.NANOS;
      case MICRO_SECONDS:
        return ChronoUnit.MICROS;
      case MILLI_SECONDS:
        return ChronoUnit.MILLIS;
      case SECONDS:
        return ChronoUnit.SECONDS;
      case MINUTES:
        return ChronoUnit.MINUTES;
      case HOURS:
        return ChronoUnit.HOURS;
      case DAYS:
        return ChronoUnit.DAYS;
      case WEEKS:
        return ChronoUnit.WEEKS;
      case MONTHS:
        return ChronoUnit.MONTHS;
      case YEARS:
        return ChronoUnit.YEARS;
      case DECADES:
        return ChronoUnit.DECADES;
      case CENTURIES:
        return ChronoUnit.CENTURIES;
      case MILLENNIA:
        return ChronoUnit.MILLENNIA;
      case ERAS:
        return ChronoUnit.ERAS;
      case FOREVER:
        return ChronoUnit.FOREVER;
    }
    throw new PacketCourierSimulationConfigurationProtoParserException(
        "Time Unit Proto not recognized.");
  }
}
