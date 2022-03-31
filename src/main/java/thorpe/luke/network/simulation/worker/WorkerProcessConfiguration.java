package thorpe.luke.network.simulation.worker;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import thorpe.luke.network.simulation.Topology;
import thorpe.luke.network.simulation.node.NodeAddress;

public class WorkerProcessConfiguration {

  private static Predicate<String> dslVariableRegexMatcher(String regexPattern) {
    return Pattern.compile("\\$\\{" + regexPattern + "\\}").asPredicate();
  }

  private static String nodesToJsonString(
      Collection<NodeAddress> nodeAddresses,
      Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap) {
    Map<String, InetAddress> nameToPublicIpMap = new HashMap<>();
    for (NodeAddress nodeAddress : nodeAddresses) {
      InetAddress publicIp = workerAddressToPublicIpMap.get(nodeAddress.asRootWorkerAddress());
      nameToPublicIpMap.put(nodeAddress.getName(), publicIp);
    }
    return "{"
        + nameToPublicIpMap
            .entrySet()
            .stream()
            .map(e -> "\"" + e.getKey() + "\": \"" + e.getValue().getHostName() + "\"")
            .collect(Collectors.joining(", "))
        + "}";
  }

  private static final Pattern NATURAL_NUMBER_REGEX_PATTERN = Pattern.compile("[1-9][0-9]*");

  private static final Predicate<String> NODE_NAME_REGEX_MATCHER =
      dslVariableRegexMatcher("NODE_NAME");
  private static final WordGenerator NODE_NAME_WORD_GENERATOR =
      (address, topology, privateIpAddress, workerAddressToPublicIpMap) -> address.getName();

  private static final Predicate<String> PUBLIC_IP_REGEX_MATCHER =
      dslVariableRegexMatcher("PUBLIC_IP");
  private static final WordGenerator PUBLIC_IP_WORD_GENERATOR =
      (address, topology, privateIpAddress, workerAddressToPublicIpMap) ->
          workerAddressToPublicIpMap.get(address.asRootWorkerAddress()).getHostName();

  private static final Predicate<String> PRIVATE_IP_REGEX_MATCHER =
      dslVariableRegexMatcher("PRIVATE_IP");
  private static final WordGenerator PRIVATE_IP_WORD_GENERATOR =
      (address, topology, privateIpAddress, workerAddressToPublicIpMap) ->
          privateIpAddress.getHostName();

  private static final Predicate<String> NEIGHBOUR_IPS_REGEX_MATCHER =
      dslVariableRegexMatcher("NEIGHBOUR_IPS");
  private static final WordGenerator NEIGHBOUR_IPS_WORD_GENERATOR =
      (address, topology, privateIpAddress, workerAddressToPublicIpMap) -> {
        Collection<NodeAddress> neighbouringNodes = topology.getNeighboursOf(address.getName());
        return nodesToJsonString(neighbouringNodes, workerAddressToPublicIpMap);
      };

  private static final Predicate<String> FLOOD_IPS_REGEX_MATCHER =
      dslVariableRegexMatcher("FLOOD_IPS_BY_DISTANCE=" + NATURAL_NUMBER_REGEX_PATTERN.pattern());
  private static final Function<Integer, WordGenerator> FLOOD_IPS_WORD_GENERATOR_FACTORY =
      distance ->
          (address, topology, privateIpAddress, workerAddressToPublicIpMap) -> {
            Collection<NodeAddress> floodNodes =
                topology.performRadialSearch(address.getName(), distance);
            return nodesToJsonString(floodNodes, workerAddressToPublicIpMap);
          };

  private static final Predicate<String> TOPOLOGY_IPS_REGEX_MATCHER =
      dslVariableRegexMatcher("TOPOLOGY_IPS");
  private static final WordGenerator TOPOLOGY_IPS_WORD_GENERATOR =
      (address, topology, privateIpAddress, workerAddressToPublicIpMap) ->
          nodesToJsonString(topology.getNodesAddresses(), workerAddressToPublicIpMap);

  private final String[] commandWords;
  private final Map<Integer, WordGenerator> indexToWordGeneratorMap;

  private WorkerProcessConfiguration(
      String[] commandWords, Map<Integer, WordGenerator> indexToWordGeneratorMap) {
    this.commandWords = commandWords;
    this.indexToWordGeneratorMap = indexToWordGeneratorMap;
  }

  public static WorkerProcessConfiguration fromCommand(String command) {
    String[] commandWords = command.split("\\s+");
    Map<Integer, WordGenerator> indexToWordGeneratorMap = new HashMap<>();
    for (int i = 0; i < commandWords.length; i++) {
      String commandWord = commandWords[i];
      if (NODE_NAME_REGEX_MATCHER.test(commandWord)) {
        indexToWordGeneratorMap.put(i, NODE_NAME_WORD_GENERATOR);
      } else if (PUBLIC_IP_REGEX_MATCHER.test(commandWord)) {
        indexToWordGeneratorMap.put(i, PUBLIC_IP_WORD_GENERATOR);
      } else if (PRIVATE_IP_REGEX_MATCHER.test(commandWord)) {
        indexToWordGeneratorMap.put(i, PRIVATE_IP_WORD_GENERATOR);
      } else if (NEIGHBOUR_IPS_REGEX_MATCHER.test(commandWord)) {
        indexToWordGeneratorMap.put(i, NEIGHBOUR_IPS_WORD_GENERATOR);
      } else if (FLOOD_IPS_REGEX_MATCHER.test(commandWord)) {
        Matcher naturalNumberMatcher = NATURAL_NUMBER_REGEX_PATTERN.matcher(commandWord);
        naturalNumberMatcher.find();
        int distance = Integer.parseInt(naturalNumberMatcher.group());
        indexToWordGeneratorMap.put(i, FLOOD_IPS_WORD_GENERATOR_FACTORY.apply(distance));
      } else if (TOPOLOGY_IPS_REGEX_MATCHER.test(commandWord)) {
        indexToWordGeneratorMap.put(i, TOPOLOGY_IPS_WORD_GENERATOR);
      }
    }
    return new WorkerProcessConfiguration(commandWords, indexToWordGeneratorMap);
  }

  public WorkerProcessFactory buildFactory(
      NodeAddress address,
      Topology topology,
      InetAddress privateIpAddress,
      Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap) {
    String[] command = new String[commandWords.length];
    for (int i = 0; i < command.length; i++) {
      WordGenerator wordGenerator = indexToWordGeneratorMap.get(i);
      if (wordGenerator != null) {
        command[i] =
            wordGenerator.generateWord(
                address, topology, privateIpAddress, workerAddressToPublicIpMap);
      } else {
        command[i] = commandWords[i];
      }
    }
    return new WorkerProcessFactory(new ProcessBuilder().command(command));
  }

  @FunctionalInterface
  private interface WordGenerator {
    String generateWord(
        NodeAddress address,
        Topology topology,
        InetAddress privateIpAddress,
        Map<WorkerAddress, InetAddress> workerAddressToPublicIpMap);
  }
}
