package client;

import network.JsonMessageCodec;
import network.MessageType;
import network.NetworkMessage;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Periodically verifies server reachability without carrying gameplay actions. */
public final class UdpHeartbeatClient implements Runnable, Closeable {
    private static final int MAX_PACKET_BYTES = 2048;
    private final InetAddress address;
    private final int port;
    private final Consumer<Boolean> statusConsumer;
    private final DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public UdpHeartbeatClient(String host, int port, Consumer<Boolean> statusConsumer) throws IOException {
        address = InetAddress.getByName(host);
        this.port = port;
        this.statusConsumer = statusConsumer == null ? ignored -> {} : statusConsumer;
        socket = new DatagramSocket();
        socket.setSoTimeout(1500);
    }

    @Override
    public void run() {
        while (running.get()) {
            boolean reachable = pingOnce();
            statusConsumer.accept(reachable);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public boolean pingOnce() {
        NetworkMessage ping = new NetworkMessage(MessageType.PING);
        byte[] request = JsonMessageCodec.encode(ping).getBytes(StandardCharsets.UTF_8);
        try {
            socket.send(new DatagramPacket(request, request.length, address, port));
            byte[] buffer = new byte[MAX_PACKET_BYTES];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            NetworkMessage pong = JsonMessageCodec.decode(new String(response.getData(),
                    response.getOffset(), response.getLength(), StandardCharsets.UTF_8));
            return pong.getType() == MessageType.PONG
                    && ping.getRequestId().equals(pong.getRequestId());
        } catch (SocketTimeoutException exception) {
            return false;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public void close() {
        running.set(false);
        socket.close();
    }
}
