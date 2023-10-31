package thorpe.luke.network.socket;

import thorpe.luke.network.packet.Packet;

import java.net.InetAddress;

public class SocketPacket {
    private final InetAddress address;
    private final Packet packet;

    public SocketPacket(InetAddress address, Packet packet) {
        this.address = address;
        this.packet = packet;
    }

    public InetAddress getAddress() {
        return address;
    }

    public Packet getPacket() {
        return packet;
    }
}
