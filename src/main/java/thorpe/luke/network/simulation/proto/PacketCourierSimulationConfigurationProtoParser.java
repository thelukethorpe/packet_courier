package thorpe.luke.network.simulation.proto;

import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import thorpe.luke.log.BufferedFileLogger;
import thorpe.luke.log.ConsoleLogger;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.NetworkEvent;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.*;
import thorpe.luke.network.simulation.node.DefaultNodeInfo;
import thorpe.luke.network.simulation.node.NodeInfoGenerator;
import thorpe.luke.network.simulation.worker.WorkerProcessConfiguration;
import thorpe.luke.network.simulation.worker.WorkerScript;
import thorpe.luke.util.UniqueStringGenerator;

public class PacketCourierSimulationConfigurationProtoParser<NodeInfo> {

  private static final DateTimeFormatter LOG_FILE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy_MM_dd_hh_mm_ss");

  private final PacketCourierSimulation.Configuration<NodeInfo> configuration;
  private final Map<String, WorkerScript<NodeInfo>> nodeNameToWorkerScriptWorkList;
  private final Random random;
  private final UniqueStringGenerator uniqueStringGenerator = new UniqueStringGenerator();

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
    if (!protobufFile.getName().endsWith(PacketCourierSimulation.CONFIGURATION_FILE_EXTENSION)) {
      throw new IOException(
          protobufFile.getName()
              + " does not have file extension "
              + PacketCourierSimulation.CONFIGURATION_FILE_EXTENSION);
    }
    PacketCourierSimulationConfigurationProto.Builder protoBuilder =
        PacketCourierSimulationConfigurationProto.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(new FileReader(protobufFile), protoBuilder);
    return protoBuilder.build();
  }

  private PacketCourierSimulation.Configuration<NodeInfo> parseConfiguration(
      PacketCourierSimulationConfigurationProto configurationProto) {
    if (configurationProto.getWallClockEnabled()) {
      configuration.usingWallClock();
    }
    if (configurationProto.getProcessLoggingEnabled()) {
      configuration.withProcessLoggingEnabled();
    }
    if (configurationProto.hasPort()) {
      configuration.withPort(configurationProto.getPort());
    }
    if (configurationProto.hasDatagramBufferSize()) {
      configuration.withDatagramBufferSize(configurationProto.getDatagramBufferSize());
    }
    if (configurationProto.hasSimulationName()) {
      configuration.withSimulationName(configurationProto.getSimulationName());
    }

    configurationProto
        .getLoggersList()
        .forEach(loggerProto -> configuration.addLogger(parseLogger(loggerProto, "logger")));

    parseDebug(configurationProto.getDebug());
    parseTopology(configurationProto.getTopology());
    return configuration;
  }

  private Duration parseDuration(DurationProto durationProto) {
    return Duration.of(durationProto.getLength(), parseTimeUnit(durationProto.getTimeUnit()));
  }

  private void parseDebug(DebugProto debugProto) {
    if (debugProto.getProcessMonitorEnabled()) {
      configuration.withProcessMonitorEnabled();
    }
    if (debugProto.hasProcessMonitorCheckupInterval()) {
      configuration.withProcessMonitorCheckupInterval(
          parseDuration(debugProto.getProcessMonitorCheckupInterval()));
    }
    if (debugProto.hasTickDurationSampleSize()) {
      configuration.withTickDurationSampleSize(debugProto.getTickDurationSampleSize());
    }
    if (debugProto.hasCrashDumpLocation()) {
      configuration.withCrashDumpLocation(Paths.get(debugProto.getCrashDumpLocation()));
    }

    debugProto
        .getMetaLoggersList()
        .forEach(
            loggerProto -> configuration.addMetaLogger(parseLogger(loggerProto, "meta-logger")));
  }

  private Optional<String> parseTopology(TopologyProto topologyProto) {
    switch (topologyProto.getTopologyCase()) {
      case CUSTOM:
        return parseCustomTopology(topologyProto.getCustom());
      case STAR:
        return parseStarTopology(topologyProto.getStar());
      case RING:
        return parseRingTopology(topologyProto.getRing());
      case LINEARDAISYCHAIN:
        return parseLinearDaisyChainTopology(topologyProto.getLinearDaisyChain());
      case FULLYCONNECTEDMESH:
        return parseFullyConnectedMeshTopology(topologyProto.getFullyConnectedMesh());
      case JOINTMESH:
        return parseJointMeshTopology(topologyProto.getJointMesh());
      case DISJOINTMESH:
        return parseDisjointMeshTopology(topologyProto.getDisjointMesh());
    }
    throw new PacketCourierSimulationConfigurationProtoParserException(
        "Topology Proto is missing parameters.");
  }

  private Optional<String> parseCustomTopology(CustomTopologyProto customTopologyProto) {
    for (CommandNodeProto commandNodeProto : customTopologyProto.getCommandsNodesList()) {
      String name = commandNodeProto.getName();
      WorkerProcessConfiguration workerProcessConfiguration =
          parseScript(commandNodeProto.getScript());
      configuration.addNode(name, workerProcessConfiguration);
    }

    for (ConnectionProto connectionProto : customTopologyProto.getConnectionsList()) {
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

    return Optional.ofNullable(
        customTopologyProto.hasJoiningNodeName() ? customTopologyProto.getJoiningNodeName() : null);
  }

  private Optional<String> parseStarTopology(StarTopologyProto starTopologyProto) {
    String serverName = "Star Server " + uniqueStringGenerator.generateUniqueString();
    WorkerProcessConfiguration serverProcessConfiguration =
        parseScript(starTopologyProto.getServerScript());
    configuration.addNode(serverName, serverProcessConfiguration);

    if (starTopologyProto.getSize() <= 0) {
      return Optional.of(serverName);
    }

    WorkerProcessConfiguration clientWorkerProcessConfiguration =
        parseScript(starTopologyProto.getClientScript());
    PacketPipeline.Parameters packetPipelineParameters =
        PacketPipeline.parameters(
            starTopologyProto
                .getNetworkConditionsList()
                .stream()
                .map(this::parseNetworkCondition)
                .collect(Collectors.toList()));

    for (int i = 0; i < starTopologyProto.getSize(); i++) {
      String clientName =
          "Star Client " + uniqueStringGenerator.generateUniqueString() + " of " + serverName;
      configuration.addNode(clientName, clientWorkerProcessConfiguration);
      configuration.addConnection(clientName, serverName, packetPipelineParameters);
      if (!starTopologyProto.getUnidirectional()) {
        configuration.addConnection(serverName, clientName, packetPipelineParameters);
      }
    }

    return Optional.of(serverName);
  }

  private Optional<String> parseRingTopology(RingTopologyProto ringTopologyProto) {
    if (ringTopologyProto.getSize() <= 0) {
      return Optional.empty();
    }

    WorkerProcessConfiguration workerProcessConfiguration =
        parseScript(ringTopologyProto.getScript());
    PacketPipeline.Parameters packetPipelineParameters =
        PacketPipeline.parameters(
            ringTopologyProto
                .getNetworkConditionsList()
                .stream()
                .map(this::parseNetworkCondition)
                .collect(Collectors.toList()));
    List<String> names = new ArrayList<>(ringTopologyProto.getSize());

    for (int i = 0; i < ringTopologyProto.getSize(); i++) {
      String name = "Ring Client " + uniqueStringGenerator.generateUniqueString();
      names.add(name);
      configuration.addNode(name, workerProcessConfiguration);
    }

    for (int i = 0; i < names.size(); i++) {
      String currentName = names.get(i);
      String nextName = names.get((i + 1) % names.size());
      configuration.addConnection(currentName, nextName, packetPipelineParameters);
      if (!ringTopologyProto.getUnidirectional()) {
        configuration.addConnection(nextName, currentName, packetPipelineParameters);
      }
    }

    return Optional.of(names.get(0));
  }

  private Optional<String> parseLinearDaisyChainTopology(
      LinearDaisyChainTopologyProto linearDaisyChainTopologyProto) {
    if (linearDaisyChainTopologyProto.getSize() <= 0) {
      return Optional.empty();
    }

    WorkerProcessConfiguration workerProcessConfiguration =
        parseScript(linearDaisyChainTopologyProto.getScript());
    PacketPipeline.Parameters packetPipelineParameters =
        PacketPipeline.parameters(
            linearDaisyChainTopologyProto
                .getNetworkConditionsList()
                .stream()
                .map(this::parseNetworkCondition)
                .collect(Collectors.toList()));
    List<String> names = new ArrayList<>(linearDaisyChainTopologyProto.getSize());

    for (int i = 0; i < linearDaisyChainTopologyProto.getSize(); i++) {
      String name = "Linear Daisy Chain Client " + uniqueStringGenerator.generateUniqueString();
      names.add(name);
      configuration.addNode(name, workerProcessConfiguration);
    }

    for (int i = 0; i < names.size() - 1; i++) {
      String currentName = names.get(i);
      String nextName = names.get(i + 1);
      configuration.addConnection(currentName, nextName, packetPipelineParameters);
      if (!linearDaisyChainTopologyProto.getUnidirectional()) {
        configuration.addConnection(nextName, currentName, packetPipelineParameters);
      }
    }

    String lastName = names.get(names.size() - 1);
    return Optional.of(lastName);
  }

  private Optional<String> parseFullyConnectedMeshTopology(
      FullyConnectedMeshTopologyProto fullyConnectedMeshTopologyProto) {
    if (fullyConnectedMeshTopologyProto.getSize() <= 0) {
      return Optional.empty();
    }

    WorkerProcessConfiguration workerProcessConfiguration =
        parseScript(fullyConnectedMeshTopologyProto.getScript());
    PacketPipeline.Parameters packetPipelineParameters =
        PacketPipeline.parameters(
            fullyConnectedMeshTopologyProto
                .getNetworkConditionsList()
                .stream()
                .map(this::parseNetworkCondition)
                .collect(Collectors.toList()));
    List<String> names = new ArrayList<>(fullyConnectedMeshTopologyProto.getSize());

    for (int i = 0; i < fullyConnectedMeshTopologyProto.getSize(); i++) {
      String name = "Fully Connected Mesh Client " + uniqueStringGenerator.generateUniqueString();
      names.add(name);
      configuration.addNode(name, workerProcessConfiguration);
    }

    for (int i = 0; i < names.size(); i++) {
      for (int j = i + 1; j < names.size(); j++) {
        String iName = names.get(i);
        String jName = names.get(j);
        configuration.addConnection(iName, jName, packetPipelineParameters);
        configuration.addConnection(jName, iName, packetPipelineParameters);
      }
    }

    return Optional.of(names.get(0));
  }

  private Optional<String> parseJointMeshTopology(JointMeshTopologyProto jointMeshTopologyProto) {
    List<String> joiningMeshNames =
        jointMeshTopologyProto
            .getJointTopologiesList()
            .stream()
            .map(this::parseTopology)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    PacketPipeline.Parameters packetPipelineParameters =
        PacketPipeline.parameters(
            jointMeshTopologyProto
                .getNetworkConditionsList()
                .stream()
                .map(this::parseNetworkCondition)
                .collect(Collectors.toList()));

    for (int i = 0; i < joiningMeshNames.size(); i++) {
      for (int j = i + 1; j < joiningMeshNames.size(); j++) {
        String iName = joiningMeshNames.get(i);
        String jName = joiningMeshNames.get(j);
        configuration.addConnection(iName, jName, packetPipelineParameters);
        configuration.addConnection(jName, iName, packetPipelineParameters);
      }
    }

    return Optional.ofNullable(
        jointMeshTopologyProto.hasJoiningNodeName()
            ? jointMeshTopologyProto.getJoiningNodeName()
            : null);
  }

  private Optional<String> parseDisjointMeshTopology(
      DisjointMeshTopologyProto disjointMeshTopologyProto) {
    disjointMeshTopologyProto.getDisjointTopologiesList().forEach(this::parseTopology);
    return Optional.ofNullable(
        disjointMeshTopologyProto.hasJoiningNodeName()
            ? disjointMeshTopologyProto.getJoiningNodeName()
            : null);
  }

  private WorkerProcessConfiguration parseScript(ScriptProto scriptProto) {
    String command = scriptProto.getCommand();
    if (scriptProto.hasTimeout()) {
      Duration timeout = parseDuration(scriptProto.getTimeout());
      return WorkerProcessConfiguration.fromCommand(command, timeout);
    }
    return WorkerProcessConfiguration.fromCommand(command);
  }

  private Logger parseLogger(LoggerProto loggerProto, String namePrefix) {
    switch (loggerProto.getLoggerParametersCase()) {
      case CONSOLE:
        return parseConsoleLogger(loggerProto.getConsole());
      case FILE:
        return parseFileLogger(loggerProto.getFile(), namePrefix);
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

  private BufferedFileLogger parseFileLogger(FileLoggerProto fileLoggerProto, String namePrefix) {
    try {
      Path logFilePath = Paths.get(fileLoggerProto.getPath());
      if (logFilePath.toFile().isFile()) {
        return new BufferedFileLogger(logFilePath.toFile());
      }
      LocalDateTime now = LocalDateTime.now();
      String logFileName =
          configuration.getSimulationName().replaceAll("\\s+", "-")
              + "__"
              + namePrefix
              + "__"
              + LOG_FILE_DATE_FORMAT.format(now)
              + PacketCourierSimulation.LOG_FILE_EXTENSION;
      return new BufferedFileLogger(logFilePath.resolve(logFileName).toFile());
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
    return NetworkCondition.packetLimit(
        packetLimitParametersProto.getPacketLimitRate(),
        parseTimeUnit(packetLimitParametersProto.getTimeUnit()));
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
