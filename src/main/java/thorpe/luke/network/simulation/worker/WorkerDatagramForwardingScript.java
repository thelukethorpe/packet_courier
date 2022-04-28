package thorpe.luke.network.simulation.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import thorpe.luke.network.packet.Packet;

public class WorkerDatagramForwardingScript<NodeInfo> implements WorkerScript<NodeInfo> {

  private final WorkerProcessFactory workerProcessFactory;
  private final int port;
  private final InetAddress privateIpAddress;
  private final int datagramBufferSize;
  private final Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap;
  private final boolean processLoggingEnabled;

  private WorkerDatagramForwardingScript(
      WorkerProcessFactory workerProcessFactory,
      int port,
      InetAddress privateIpAddress,
      int datagramBufferSize,
      Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap,
      boolean processLoggingEnabled) {
    this.workerProcessFactory = workerProcessFactory;
    this.port = port;
    this.privateIpAddress = privateIpAddress;
    this.datagramBufferSize = datagramBufferSize;
    this.privateIpAddressToPublicSocketMap = privateIpAddressToPublicSocketMap;
    this.processLoggingEnabled = processLoggingEnabled;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void run(WorkerManager<NodeInfo> workerManager) {
    Thread forwardingThread =
        new Thread(
            () -> {
              byte[] buffer = new byte[datagramBufferSize];
              do {
                Packet packet;
                try {
                  packet = workerManager.waitForMail();
                } catch (WorkerException e) {
                  return;
                }
                List<Byte> data = packet.getData();
                for (int i = 0; i < buffer.length; i++) {
                  buffer[i] = data.get(i);
                }
                byte[] sourceIpAddress = new byte[data.size() - datagramBufferSize];
                for (int i = 0; i < sourceIpAddress.length; i++) {
                  sourceIpAddress[i] = data.get(datagramBufferSize + i);
                }
                DatagramPacket forwardingDatagramPacket =
                    new DatagramPacket(buffer, buffer.length, privateIpAddress, port);
                try {
                  DatagramSocket publicSocket =
                      privateIpAddressToPublicSocketMap.get(
                          InetAddress.getByAddress(sourceIpAddress));
                  publicSocket.send(forwardingDatagramPacket);
                } catch (IOException e) {
                  List<String> stackTraceErrors =
                      Arrays.stream(e.getStackTrace())
                          .map(StackTraceElement::toString)
                          .collect(Collectors.toList());
                  workerManager.generateCrashDump(stackTraceErrors);
                  return;
                }
              } while (true);
            });
    forwardingThread.start();
    try {
      Process process = workerProcessFactory.start();
      int exitStatus = process.waitFor();
      forwardingThread.interrupt();
      forwardingThread.join();
      if (exitStatus != 0) {
        List<String> processErrors = collectStringsFromStream(process.getErrorStream());
        workerManager.generateCrashDump(processErrors);
      } else if (processLoggingEnabled) {
        collectStringsFromStream(process.getInputStream()).forEach(workerManager::log);
      }
    } catch (IOException | InterruptedException e) {
      throw new WorkerException(e);
    }
  }

  private static List<String> collectStringsFromStream(InputStream inputStream) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    List<String> strings = new LinkedList<>();
    String line = bufferedReader.readLine();
    while (line != null) {
      strings.add(line);
      line = bufferedReader.readLine();
    }
    return strings;
  }

  public static class Builder {
    private WorkerProcessFactory workerProcessFactory;
    private int port;
    private InetAddress privateIpAddress;
    private int datagramBufferSize;
    private Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap;
    private boolean processLoggingEnabled;

    private Builder() {}

    public Builder withWorkerProcessFactory(WorkerProcessFactory workerProcessFactory) {
      this.workerProcessFactory = workerProcessFactory;
      return this;
    }

    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public Builder withPrivateIpAddress(InetAddress privateIpAddress) {
      this.privateIpAddress = privateIpAddress;
      return this;
    }

    public Builder withDatagramBufferSize(int datagramBufferSize) {
      this.datagramBufferSize = datagramBufferSize;
      return this;
    }

    public Builder withPrivateIpAddressToPublicSocketMap(
        Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap) {
      this.privateIpAddressToPublicSocketMap = privateIpAddressToPublicSocketMap;
      return this;
    }

    public Builder withProcessLoggingEnabled(boolean processLoggingEnabled) {
      this.processLoggingEnabled = processLoggingEnabled;
      return this;
    }

    public <NodeInfo> WorkerDatagramForwardingScript<NodeInfo> build() {
      return new WorkerDatagramForwardingScript<>(
          workerProcessFactory,
          port,
          privateIpAddress,
          datagramBufferSize,
          privateIpAddressToPublicSocketMap,
          processLoggingEnabled);
    }
  }
}
