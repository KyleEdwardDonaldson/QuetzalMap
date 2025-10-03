package dev.ked.quetzalmap.server.sse;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Represents a single Server-Sent Events connection.
 * Handles sending events to a connected client.
 */
public final class SSEConnection {
    private static final Logger LOGGER = Logger.getLogger(SSEConnection.class.getName());

    private final int id;
    private final HttpServerExchange exchange;
    private final OutputStream outputStream;
    private final AtomicBoolean closed;

    public SSEConnection(int id, HttpServerExchange exchange) throws IOException {
        this.id = id;
        this.exchange = exchange;
        this.closed = new AtomicBoolean(false);

        // Set SSE headers
        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream");
        exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");
        exchange.getResponseHeaders().put(Headers.CONNECTION, "keep-alive");
        exchange.setPersistent(true);

        // Start blocking mode
        exchange.startBlocking();
        this.outputStream = exchange.getOutputStream();

        // Send initial comment to establish connection
        outputStream.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /**
     * Send an SSE event to the client.
     *
     * @param event Event name
     * @param data  Event data (JSON string)
     * @return true if sent successfully
     */
    public synchronized boolean sendEvent(String event, String data) {
        if (closed.get()) {
            return false;
        }

        try {
            String message = String.format("event: %s\ndata: %s\n\n", event, data);
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            return true;

        } catch (IOException e) {
            LOGGER.warning("Error sending SSE event to connection " + id + ": " + e.getMessage());
            close();
            return false;
        }
    }

    /**
     * Send an SSE comment (keepalive).
     */
    public synchronized boolean sendComment(String comment) {
        if (closed.get()) {
            return false;
        }

        try {
            String message = String.format(": %s\n\n", comment);
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            return true;

        } catch (IOException e) {
            LOGGER.fine("Error sending SSE comment to connection " + id + ": " + e.getMessage());
            close();
            return false;
        }
    }

    /**
     * Close the connection.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                outputStream.close();
            } catch (Exception e) {
                // Already closed
            }
            try {
                exchange.endExchange();
            } catch (Exception e) {
                // Already closed
            }
        }
    }

    /**
     * Check if connection is closed.
     */
    public boolean isClosed() {
        return closed.get();
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("SSEConnection{id=%d, closed=%s}", id, closed.get());
    }
}
