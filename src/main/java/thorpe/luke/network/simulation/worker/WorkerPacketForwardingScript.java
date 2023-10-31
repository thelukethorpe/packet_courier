package thorpe.luke.network.simulation.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.socket.Socket;
import thorpe.luke.network.socket.SocketPacket;
import thorpe.luke.util.error.ExceptionListener;

public class WorkerPacketForwardingScript implements WorkerScript {

  private final int port;
  private final InetAddress privateIpAddress;
  private final int bufferSize;
  private final Map<InetAddress, Socket> privateIpAddressToPublicSocketMap;
  private final ExceptionListener exceptionListener;

  private WorkerPacketForwardingScript(
      int port,
      InetAddress privateIpAddress,
      int bufferSize,
      Map<InetAddress, Socket> privateIpAddressToPublicSocketMap,
      ExceptionListener exceptionListener) {
    this.port = port;
    this.privateIpAddress = privateIpAddress;
    this.bufferSize = bufferSize;
    this.privateIpAddressToPublicSocketMap = privateIpAddressToPublicSocketMap;
    this.exceptionListener = exceptionListener;
  }

  public static Builder builder() {
    return new Builder();
  }

  private void forwardPacket(Packet packet) {
    List<Byte> data = packet.getData();
    byte[] sourceIpAddress = new byte[data.size() - bufferSize];
    for (int i = 0; i < sourceIpAddress.length; i++) {
      sourceIpAddress[i] = data.get(bufferSize + i);
    }
    try {
    Socket publicSocket =
            privateIpAddressToPublicSocketMap.get(InetAddress.getByAddress(sourceIpAddress));
      if (publicSocket != null) {
        publicSocket.write(new SocketPacket(privateIpAddress, packet));
      }
    } catch (IOException e) {
      exceptionListener.invoke(e);
    }
  }

  @Override
  public void tick(WorkerManager workerManager) {
    workerManager.getMail().ifPresent(this::forwardPacket);
  }

  public static class Builder {
    private int port;
    private InetAddress privateIpAddress;
    private int bufferSize;
    private Map<InetAddress, Socket> privateIpAddressToPublicSocketMap;
    private ExceptionListener exceptionListener;
    private Logger logger;

    private Builder() {}


    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public Builder withPrivateIpAddress(InetAddress privateIpAddress) {
      this.privateIpAddress = privateIpAddress;
      return this;
    }

    public Builder withBufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    public Builder withPrivateIpAddressToPublicSocketMap(
        Map<InetAddress, Socket> privateIpAddressToPublicSocketMap) {
      this.privateIpAddressToPublicSocketMap = privateIpAddressToPublicSocketMap;
      return this;
    }


    public Builder withExceptionListener(ExceptionListener exceptionListener) {
      this.exceptionListener = exceptionListener;
      return this;
    }

    public WorkerPacketForwardingScript build() {
      return new WorkerPacketForwardingScript(
          port,
          privateIpAddress,
              bufferSize,
          privateIpAddressToPublicSocketMap,
          exceptionListener);
    }
  }
}
