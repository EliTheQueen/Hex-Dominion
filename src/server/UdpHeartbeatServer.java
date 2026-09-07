package server;

import network.JsonMessageCodec;
import network.MessageType;
import network.NetworkMessage;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** UDP is intentionally restricted to best-effort JSON PING/PONG health checks. */
public final class UdpHeartbeatServer implements Runnable, Closeable {
    public static final int UDP_PORT = 8083;
    private static final int MAX_PACKET_BYTES = 2048;

    private final DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public UdpHeartbeatServer() throws SocketException { this(UDP_PORT); }

    public UdpHeartbeatServer(int port) throws SocketException {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("invalid UDP port");
        socket = new DatagramSocket(port);
        socket.setSoTimeout(1000);
    }

    @Override
    public void run() {
        System.out.println("UDP heartbeat started on port " + getPort());
        byte[] buffer = new byte[MAX_PACKET_BYTES];
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String json = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                        StandardCharsets.UTF_8);
                NetworkMessage request = JsonMessageCodec.decode(json);
                if (request.getType() != MessageType.PING) continue;
                byte[] response = JsonMessageCodec.encode(NetworkMessage.response(
                        MessageType.PONG, request.getRequestId(), new HeartbeatPayload(
                                System.currentTimeMillis()))).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(response, response.length,
                        packet.getAddress(), packet.getPort()));
            } catch (SocketTimeoutException ignored) {
                // Periodically wake so close() can stop the loop promptly.
            } catch (IllegalArgumentException ignored) {
                // Invalid UDP packets are untrusted and never affect the game server.
            } catch (IOException exception) {
                if (running.get()) System.out.println("UDP heartbeat error: " + exception.getMessage());
            }
        }
    }

    public int getPort() { return socket.getLocalPort(); }

    @Override
    public void close() {
        running.set(false);
        socket.close();
    }

    private static final class HeartbeatPayload {
        private final long serverTime;
        private HeartbeatPayload(long serverTime) { this.serverTime = serverTime; }
    }
}
