package dev.ked.quetzalmap.server.sse;

import io.undertow.server.HttpServerExchange;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Manages Server-Sent Events (SSE) connections for live map updates.
 * Broadcasts tile updates to all connected clients.
 */
public final class SSEManager {
    private static final Logger LOGGER = Logger.getLogger(SSEManager.class.getName());

    private final Set<SSEConnection> connections;
    private final AtomicInteger connectionIdCounter;
    private Runnable onNewConnection;

    public SSEManager() {
        this.connections = ConcurrentHashMap.newKeySet();
        this.connectionIdCounter = new AtomicInteger(0);
    }

    /**
     * Set callback to run when a new connection is established.
     */
    public void setOnNewConnection(Runnable callback) {
        this.onNewConnection = callback;
    }

    /**
     * Register a new SSE connection.
     */
    public SSEConnection registerConnection(HttpServerExchange exchange) throws java.io.IOException {
        int connectionId = connectionIdCounter.incrementAndGet();
        SSEConnection connection = new SSEConnection(connectionId, exchange);

        connections.add(connection);
        LOGGER.info("SSE connection registered: " + connectionId + " (total: " + connections.size() + ", isClosed=" + connection.isClosed() + ")");

        // Send initial connection event
        connection.sendEvent("connected", String.format("{\"id\":%d}", connectionId));

        // Trigger callback to send initial data (e.g., player list)
        if (onNewConnection != null) {
            try {
                onNewConnection.run();
            } catch (Exception e) {
                LOGGER.warning("Error in onNewConnection callback: " + e.getMessage());
            }
        }

        return connection;
    }

    /**
     * Unregister a connection.
     */
    public void unregisterConnection(SSEConnection connection) {
        connections.remove(connection);
        LOGGER.info("SSE connection unregistered: " + connection.getId() + " (total: " + connections.size() + ")");
    }

    /**
     * Broadcast a tile update to all connected clients.
     */
    public void broadcastTileUpdate(String world, int zoom, int x, int z) {
        String data = String.format("{\"type\":\"tile_update\",\"world\":\"%s\",\"zoom\":%d,\"x\":%d,\"z\":%d}",
                world, zoom, x, z);

        broadcast("tile_update", data);
    }

    /**
     * Broadcast a message to all connected clients.
     */
    public void broadcast(String event, String data) {
        // Remove closed connections
        int beforeRemove = connections.size();
        connections.removeIf(SSEConnection::isClosed);
        int afterRemove = connections.size();
        if (beforeRemove != afterRemove) {
            LOGGER.fine(String.format("Removed %d closed connections (%d -> %d)",
                    beforeRemove - afterRemove, beforeRemove, afterRemove));
        }

        // Send to all active connections
        int sent = 0;
        for (SSEConnection connection : connections) {
            if (connection.sendEvent(event, data)) {
                sent++;
            }
        }

        LOGGER.fine(String.format("Broadcast '%s' to %d/%d connections", event, sent, connections.size()));
    }

    /**
     * Send a keepalive ping to all connections.
     */
    public void sendKeepalive() {
        connections.removeIf(SSEConnection::isClosed);

        for (SSEConnection connection : connections) {
            connection.sendComment("keepalive");
        }
    }

    /**
     * Get the number of active connections.
     */
    public int getConnectionCount() {
        connections.removeIf(SSEConnection::isClosed);
        return connections.size();
    }

    /**
     * Close all connections and shutdown.
     */
    public void shutdown() {
        LOGGER.info("Closing all SSE connections (" + connections.size() + ")");

        for (SSEConnection connection : connections) {
            connection.close();
        }

        connections.clear();
    }

    /**
     * Get statistics.
     */
    public Stats getStats() {
        connections.removeIf(SSEConnection::isClosed);
        return new Stats(connections.size(), connectionIdCounter.get());
    }

    public record Stats(int activeConnections, int totalConnections) {
        @Override
        public String toString() {
            return String.format("SSEManager{active=%d, total=%d}", activeConnections, totalConnections);
        }
    }
}
