package com.parking.qa.tunnel;

import com.parking.qa.config.TestConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class SshTunnelManager {
    private static Process tunnelProcess;

    private SshTunnelManager() {
    }

    public static synchronized void startIfEnabled() {
        if (!TestConfig.dbTunnelEnabled()) {
            return;
        }

        if (isLocalPortOpen()) {
            System.out.println("DB SSH tunnel already open on "
                    + TestConfig.dbTunnelLocalHost() + ":" + TestConfig.dbTunnelLocalPort());
            return;
        }

        Path keyPath = Path.of(TestConfig.dbTunnelKeyPath());
        if (!Files.exists(keyPath)) {
            throw new IllegalStateException("SSH key not found: " + keyPath);
        }

        List<String> command = List.of(
                "ssh",
                "-i", keyPath.toString(),
                "-o", "StrictHostKeyChecking=accept-new",
                "-o", "ExitOnForwardFailure=yes",
                "-o", "ServerAliveInterval=30",
                "-o", "ServerAliveCountMax=3",
                "-N",
                "-L", tunnelForward(),
                TestConfig.dbTunnelEc2User() + "@" + TestConfig.dbTunnelEc2Host()
        );

        try {
            System.out.println("Starting DB SSH tunnel for Maven test run...");
            System.out.println("Forward: " + TestConfig.dbTunnelLocalHost() + ":"
                    + TestConfig.dbTunnelLocalPort() + " -> "
                    + TestConfig.dbTunnelRdsHost() + ":" + TestConfig.dbTunnelRdsPort());

            tunnelProcess = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            waitUntilOpen(Duration.ofSeconds(15));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start DB SSH tunnel", ex);
        }
    }

    public static synchronized void stopIfStartedByTests() {
        if (tunnelProcess == null) {
            return;
        }

        System.out.println("Stopping DB SSH tunnel started by tests...");
        tunnelProcess.destroy();
        tunnelProcess = null;
    }

    private static String tunnelForward() {
        return TestConfig.dbTunnelLocalHost() + ":"
                + TestConfig.dbTunnelLocalPort() + ":"
                + TestConfig.dbTunnelRdsHost() + ":"
                + TestConfig.dbTunnelRdsPort();
    }

    private static void waitUntilOpen(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isLocalPortOpen()) {
                System.out.println("DB SSH tunnel is open.");
                return;
            }

            if (tunnelProcess != null && !tunnelProcess.isAlive()) {
                throw new IllegalStateException("SSH tunnel process exited before the local port opened.");
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for DB SSH tunnel", ex);
            }
        }

        throw new IllegalStateException("Timed out waiting for DB SSH tunnel on "
                + TestConfig.dbTunnelLocalHost() + ":" + TestConfig.dbTunnelLocalPort());
    }

    private static boolean isLocalPortOpen() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(
                    TestConfig.dbTunnelLocalHost(),
                    TestConfig.dbTunnelLocalPort()
            ), 1000);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
