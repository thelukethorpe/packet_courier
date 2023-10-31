package thorpe.luke.network.socket;

import java.net.InetAddress;
import java.util.Optional;

public interface Socket {
    Optional<SocketPacket> read();

    void write(SocketPacket socketPacket);

    InetAddress getAddress();
}
