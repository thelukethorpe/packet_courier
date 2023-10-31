package thorpe.luke.network.socket;

import java.net.InetAddress;

@FunctionalInterface
public interface SocketFactory {
    Socket getSocket(InetAddress address);
}
