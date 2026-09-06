import client.NetworkEventListener;
import client.NetworkManager;
import client.UdpHeartbeatClient;
import model.Builder;
import model.Constants;
import model.GameState;
import model.HexCoordinate;
import model.Unit;
import network.GameStateSnapshotCodec;
import server.GameServer;
import server.UdpHeartbeatServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Real socket coverage for controller/observer synchronization and disconnect safety. */
public final class NetworkIntegrationTest {
    public static void main(String[] args) throws Exception {
        GameState roundTrip = GameStateSnapshotCodec.decode(
                GameStateSnapshotCodec.encode(new GameState(15, 13, 991L)));
        require(roundTrip.getCurrentTurn() == 1, "state snapshot must round-trip");
        GameServer server = new GameServer(0);
        Thread tcpThread = new Thread(() -> {
            try { server.start(); } catch (Exception exception) {
                if (!Thread.currentThread().isInterrupted()) exception.printStackTrace();
            }
        }, "integration-tcp-server");
        tcpThread.setDaemon(true);
        tcpThread.start();
        int tcpPort = awaitTcpPort(server);

        UdpHeartbeatServer heartbeatServer = new UdpHeartbeatServer(0);
        Thread udpThread = new Thread(heartbeatServer, "integration-udp-server");
        udpThread.setDaemon(true);
        udpThread.start();

        NetworkManager controller = new NetworkManager("127.0.0.1", tcpPort,
                heartbeatServer.getPort());
        NetworkManager observer = new NetworkManager("127.0.0.1", tcpPort,
                heartbeatServer.getPort());
        Probe controllerProbe = new Probe();
        Probe observerProbe = new Probe();
        controller.addListener(controllerProbe);
        observer.addListener(observerProbe);

        try {
            controller.connect();
            require(controllerProbe.hello.await(3, TimeUnit.SECONDS), "controller HELLO timed out");
            require(controllerProbe.controllerRole, "first client must be controller");
            observer.connect();
            require(observerProbe.hello.await(3, TimeUnit.SECONDS), "observer HELLO timed out");
            require(!observerProbe.controllerRole, "second client must be observer");

            try (UdpHeartbeatClient heartbeat = new UdpHeartbeatClient("127.0.0.1",
                    heartbeatServer.getPort(), ignored -> {})) {
                require(heartbeat.pingOnce(), "UDP PING must receive matching PONG");
            }

            controller.startGame(15, 13, 991L);
            GameState initial = awaitRevision(controllerProbe, observerProbe, 1);
            require(initial.getCurrentTurn() == 1, "new game starts on turn 1");

            MoveFixture move = findMove(initial);
            controller.moveUnit(move.unitIndex, move.destination);
            GameState moved = awaitRevision(controllerProbe, observerProbe, 2);
            require(moved.getPlayer().getUnits().get(move.unitIndex).getPosition().equals(move.destination),
                    "server must execute movement before broadcasting revision 2");

            controller.moveUnit(9999, move.destination);
            require(controllerProbe.error.await(3, TimeUnit.SECONDS), "invalid move must return ERROR");
            require("UNIT_NOT_FOUND".equals(controllerProbe.errorCode.get()),
                    "invalid move must return a specific error code");

            BuildFixture build = findBuild(moved);
            controller.build(build.builderIndex, build.type, build.coordinate);
            GameState built = awaitRevision(controllerProbe, observerProbe, 3);
            require(built.getPlayer().getBuildingAt(build.coordinate) != null,
                    "server must construct the requested building");

            controller.endTurn();
            GameState ended = awaitRevision(controllerProbe, observerProbe, 4);
            require(ended.getCurrentTurn() == 2, "server must advance the turn");

            observer.disconnect();
            awaitClientCount(server, 1);
            controller.endTurn();
            awaitSingleRevision(controllerProbe, 5);
            require(controller.isConnected(), "remaining client must survive observer disconnect");
        } finally {
            controller.disconnect();
            observer.disconnect();
            heartbeatServer.close();
            server.close();
        }
        System.out.println("NetworkIntegrationTest passed");
    }

    private static int awaitTcpPort(GameServer server) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (server.getPort() == 0 && System.nanoTime() < deadline) Thread.sleep(10);
        require(server.getPort() != 0, "TCP server did not bind");
        return server.getPort();
    }

    private static void awaitClientCount(GameServer server, int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (server.getConnectedClientCount() != expected && System.nanoTime() < deadline) Thread.sleep(10);
        require(server.getConnectedClientCount() == expected, "server did not remove disconnected client");
    }

    private static GameState awaitRevision(Probe first, Probe second, int revision) throws InterruptedException {
        return awaitRevisions(List.of(first, second), revision);
    }

    private static GameState awaitSingleRevision(Probe probe, int revision) throws InterruptedException {
        return awaitRevisions(List.of(probe), revision);
    }

    private static GameState awaitRevisions(List<Probe> probes, int revision) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            boolean complete = true;
            for (Probe probe : probes) complete &= probe.revision.get() >= revision;
            if (complete) return probes.get(0).state.get();
            Thread.sleep(10);
        }
        throw new AssertionError("STATE_UPDATE revision " + revision + " timed out");
    }

    private static MoveFixture findMove(GameState state) {
        List<Unit> units = state.getPlayer().getUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            for (HexCoordinate neighbour : unit.getPosition().findNeighbours()) {
                if (state.getPlayer().getUnitAt(neighbour) == null
                        && unit.canMoveTo(state.getMap(), neighbour, state.getMovementPolicy())) {
                    return new MoveFixture(i, neighbour);
                }
            }
        }
        throw new AssertionError("deterministic state has no movement fixture");
    }

    private static BuildFixture findBuild(GameState state) {
        List<Unit> units = state.getPlayer().getUnits();
        for (int i = 0; i < units.size(); i++) {
            if (!(units.get(i) instanceof Builder)) continue;
            List<HexCoordinate> sites = new ArrayList<>();
            sites.add(units.get(i).getPosition());
            sites.addAll(units.get(i).getPosition().findNeighbours());
            for (Constants.BuildingType type : List.of(Constants.BuildingType.LUMBER_MILL,
                    Constants.BuildingType.FARM, Constants.BuildingType.STABLE,
                    Constants.BuildingType.MONUMENT)) {
                if (!state.getPlayer().canAfford(state.getBuildCost(type))) continue;
                for (HexCoordinate site : sites) {
                    if (state.getBuildSiteAvailability(site, type).isEnabled()) {
                        return new BuildFixture(i, type, site);
                    }
                }
            }
        }
        throw new AssertionError("deterministic state has no build fixture");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Probe implements NetworkEventListener {
        private final CountDownLatch hello = new CountDownLatch(1);
        private final CountDownLatch error = new CountDownLatch(1);
        private final AtomicInteger revision = new AtomicInteger();
        private final AtomicReference<GameState> state = new AtomicReference<>();
        private final AtomicReference<String> errorCode = new AtomicReference<>();
        private volatile boolean controllerRole;

        @Override public void onHello(int clientId, boolean controller) {
            controllerRole = controller;
            hello.countDown();
        }

        @Override public void onStateUpdate(GameState gameState, long newRevision) {
            state.set(gameState);
            revision.set((int) newRevision);
        }

        @Override public void onError(String code, String message) {
            errorCode.set(code);
            error.countDown();
        }
    }

    private record MoveFixture(int unitIndex, HexCoordinate destination) {}
    private record BuildFixture(int builderIndex, Constants.BuildingType type,
                                HexCoordinate coordinate) {}
}
