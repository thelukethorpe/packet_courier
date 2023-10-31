package thorpe.luke.network.socket;

import java.net.InetAddress;
import java.util.Optional;

public interface Socket {
    Optional<SocketPacket> receive();

    void send(SocketPacket socketPacket);

    InetAddress getAddress();
}
