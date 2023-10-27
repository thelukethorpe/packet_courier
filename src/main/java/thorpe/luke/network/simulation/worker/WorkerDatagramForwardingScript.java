package thorpe.luke.network.simulation.worker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.util.ThreadNameGenerator;
import thorpe.luke.util.error.ExceptionListener;

public class WorkerDatagramForwardingScript implements WorkerScript {

  private final WorkerProcess.Factory workerProcessFactory;
  private final int port;
  private final InetAddress privateIpAddress;
  private final int datagramBufferSize;
  private final Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap;
  private final boolean processLoggingEnabled;
  private final WorkerProcessMonitor workerProcessMonitor;
  private final ExceptionListener exceptionListener;
  private final Logger logger;

  private WorkerDatagramForwardingScript(
      WorkerProcess.Factory workerProcessFactory,
      int port,
      InetAddress privateIpAddress,
      int datagramBufferSize,
      Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap,
      boolean processLoggingEnabled,
      WorkerProcessMonitor workerProcessMonitor,
      ExceptionListener exceptionListener,
      Logger logger) {
    this.workerProcessFactory = workerProcessFactory;
    this.port = port;
    this.privateIpAddress = privateIpAddress;
    this.datagramBufferSize = datagramBufferSize;
    this.privateIpAddressToPublicSocketMap = privateIpAddressToPublicSocketMap;
    this.processLoggingEnabled = processLoggingEnabled;
    this.workerProcessMonitor = workerProcessMonitor;
    this.exceptionListener = exceptionListener;
    this.logger = logger;
  }

  public static Builder builder() {
    return new Builder();
  }

  private void waitForAndForwardPackets(WorkerManager workerManager) {
    byte[] buffer = new byte[datagramBufferSize];
    do {
      Packet packet;
      try {
        packet = workerManager.waitForMail();
      } catch (WorkerException e) {
        exceptionListener.invoke(e);
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
            privateIpAddressToPublicSocketMap.get(InetAddress.getByAddress(sourceIpAddress));
        if (publicSocket != null) {
          publicSocket.send(forwardingDatagramPacket);
        }
      } catch (IOException e) {
        exceptionListener.invoke(e);
        return;
      }
    } while (true);
  }

  @Override
  public void run(WorkerManager workerManager) {
    Thread forwardingThread =
        new Thread(
            () -> waitForAndForwardPackets(workerManager),
            ThreadNameGenerator.generateThreadName(
                "Packet Forwarder at " + workerManager.getAddress().getName()));
    forwardingThread.start();
    try {
      String workerProcessName = workerManager.getAddress().getHostingNodeAddress().getName();
      WorkerProcess workerProcess;
      if (processLoggingEnabled) {
        workerProcess = workerProcessFactory.start(logger);
      } else {
        workerProcess = workerProcessFactory.start();
      }
      workerProcessMonitor.addProcess(workerProcessName, workerProcess);
      WorkerProcessExitStatus workerProcessExitStatus = workerProcess.waitFor();
      forwardingThread.interrupt();
      forwardingThread.join();
      if (!workerProcessExitStatus.isSuccess()) {
        throw new WorkerException(String.join("\n", workerProcessExitStatus.getErrors()));
      }
    } catch (IOException | InterruptedException e) {
      throw new WorkerException(e);
    }
  }

  public static class Builder {
    private WorkerProcess.Factory workerProcessFactory;
    private int port;
    private InetAddress privateIpAddress;
    private int datagramBufferSize;
    private Map<InetAddress, DatagramSocket> privateIpAddressToPublicSocketMap;
    private boolean processLoggingEnabled;
    private WorkerProcessMonitor workerProcessMonitor;
    private ExceptionListener exceptionListener;
    private Logger logger;

    private Builder() {}

    public Builder withWorkerProcessFactory(WorkerProcess.Factory workerProcessFactory) {
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

    public Builder withProcessMonitor(WorkerProcessMonitor workerProcessMonitor) {
      this.workerProcessMonitor = workerProcessMonitor;
      return this;
    }

    public Builder withExceptionListener(ExceptionListener exceptionListener) {
      this.exceptionListener = exceptionListener;
      return this;
    }

    public Builder withLogger(Logger logger) {
      this.logger = logger;
      return this;
    }

    public WorkerDatagramForwardingScript build() {
      return new WorkerDatagramForwardingScript(
          workerProcessFactory,
          port,
          privateIpAddress,
          datagramBufferSize,
          privateIpAddressToPublicSocketMap,
          processLoggingEnabled,
          workerProcessMonitor,
          exceptionListener,
          logger);
    }
  }
}
