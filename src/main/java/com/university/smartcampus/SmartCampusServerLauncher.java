package com.university.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import jakarta.ws.rs.ProcessingException;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

public final class SmartCampusServerLauncher {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "8080";
    private static final String SERVER_ROOT = "/";
    private static final String API_PATH = "/api/v1";
    private static final int MAX_PORT_RETRIES = 20;

    private SmartCampusServerLauncher() {
    }

    public static void main(String[] args) throws IOException {
        String host = System.getProperty("smartcampus.host", DEFAULT_HOST);
        String configuredPort = System.getProperty("smartcampus.port", DEFAULT_PORT);
        boolean explicitPort = System.getProperty("smartcampus.port") != null;
        int startPort = parsePort(configuredPort);

        HttpServer server = null;
        String serverUri = null;
        String apiUri = null;

        for (int candidate = startPort; candidate <= startPort + MAX_PORT_RETRIES; candidate++) {
            serverUri = "http://" + host + ":" + candidate + SERVER_ROOT;
            apiUri = "http://" + host + ":" + candidate + API_PATH;
            try {
                server = GrizzlyHttpServerFactory.createHttpServer(
                        URI.create(serverUri),
                        new SmartCampusApplication()
                );
                break;
            } catch (ProcessingException processingException) {
                if (isBindException(processingException) && !explicitPort && candidate < startPort + MAX_PORT_RETRIES) {
                    System.out.println("Port " + candidate + " is in use. Retrying on next port...");
                    continue;
                }
                if (isBindException(processingException) && explicitPort) {
                    throw new IOException("Port " + candidate + " is already in use. Set a different value with -Dsmartcampus.port=<port>.", processingException);
                }
                throw processingException;
            }
        }

        if (server == null || apiUri == null) {
            throw new IOException("Could not start server. No available port found in range " + startPort + "-" + (startPort + MAX_PORT_RETRIES) + ".");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        System.out.println("Smart Campus API started at " + apiUri);

        if (Boolean.getBoolean("smartcampus.waitForEnter")) {
            System.out.println("Press Enter to stop the server.");
            readUntilEnter(System.in);
            server.shutdownNow();
            return;
        }

        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        }
    }

    private static void readUntilEnter(InputStream inputStream) throws IOException {
        while (true) {
            int next = inputStream.read();
            if (next == -1 || next == '\n' || next == '\r') {
                return;
            }
        }
    }

    private static int parsePort(String value) throws IOException {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 65535) {
                throw new IOException("Invalid port " + value + ". Use a value between 1 and 65535.");
            }
            return parsed;
        } catch (NumberFormatException numberFormatException) {
            throw new IOException("Invalid port " + value + ". Use a numeric value.", numberFormatException);
        }
    }

    private static boolean isBindException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BindException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
