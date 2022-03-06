package thorpe.luke.network.simulation;

import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import thorpe.luke.network.packet.NetworkCondition;
import thorpe.luke.network.packet.NetworkEvent;
import thorpe.luke.network.packet.PacketPipeline;
import thorpe.luke.network.simulation.node.NodeInfoGenerator;
import thorpe.luke.network.simulation.worker.WorkerScript;

public class DistributedNetworkSimulationConfigurationProtoParser<NodeInfo> {

  private final DistributedNetworkSimulation.Configuration<NodeInfo> configuration;
  private final Map<String, WorkerScript<NodeInfo>> nodeNameToWorkerScriptWorkList;
  private final Random random;

  private DistributedNetworkSimulationConfigurationProtoParser(
      DistributedNetworkSimulation.Configuration<NodeInfo> configuration,
      Map<String, WorkerScript<NodeInfo>> nodeNameToWorkerScriptWorkList,
      Random random) {
    this.configuration = configuration;
    this.nodeNameToWorkerScriptWorkList = nodeNameToWorkerScriptWorkList;
    this.random = random;
  }

  public static <NodeInfo> DistributedNetworkSimulation.Configuration<NodeInfo> parse(
      File protobufFile,
      NodeInfoGenerator<NodeInfo> nodeInfoGenerator,
      Map<String, WorkerScript<NodeInfo>> nodeNameToWorkerScriptMap) {
    DistributedNetworkSimulationConfigurationProto configurationProto;
    try {
      configurationProto = readProtobufFile(protobufFile);
    } catch (IOException e) {
      throw new DistributedNetworkSimulationConfigurationProtoParserException(e);
    }
    DistributedNetworkSimulation.Configuration<NodeInfo> configuration =
        DistributedNetworkSimulation.configuration(nodeInfoGenerator);
    Random random =
        configurationProto.hasSeed() ? new Random(configurationProto.getSeed()) : new Random();
    DistributedNetworkSimulationConfigurationProtoParser<NodeInfo> parser =
        new DistributedNetworkSimulationConfigurationProtoParser<>(
            configuration, new HashMap<>(nodeNameToWorkerScriptMap), random);
    return parser.parseConfiguration(configurationProto);
  }

  private static DistributedNetworkSimulationConfigurationProto readProtobufFile(File protobufFile)
      throws IOException {
    DistributedNetworkSimulationConfigurationProto.Builder protoBuilder =
        DistributedNetworkSimulationConfigurationProto.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(new FileReader(protobufFile), protoBuilder);
    return protoBuilder.build();
  }

  private DistributedNetworkSimulation.Configuration<NodeInfo> parseConfiguration(
      DistributedNetworkSimulationConfigurationProto configurationProto) {
    if (configurationProto.hasWallClockEnabled() && configurationProto.getWallClockEnabled()) {
      configuration.usingWallClock();
    }
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
    throw new DistributedNetworkSimulationConfigurationProtoParserException(
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
    throw new DistributedNetworkSimulationConfigurationProtoParserException(
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
    throw new DistributedNetworkSimulationConfigurationProtoParserException(
        "Time Unit Proto not recognized.");
  }
}
