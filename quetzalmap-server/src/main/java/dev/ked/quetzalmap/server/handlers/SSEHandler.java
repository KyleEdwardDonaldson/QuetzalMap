package dev.ked.quetzalmap.server.handlers;

import dev.ked.quetzalmap.server.sse.SSEConnection;
import dev.ked.quetzalmap.server.sse.SSEManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.util.logging.Logger;

/**
 * HTTP handler for Server-Sent Events.
 * Manages long-lived connections for real-time map updates.
 */
public final class SSEHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(SSEHandler.class.getName());

    private final SSEManager sseManager;

    public SSEHandler(SSEManager sseManager) {
        this.sseManager = sseManager;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        // Only accept GET requests
        if (!exchange.getRequestMethod().toString().equals("GET")) {
            exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Only GET method is allowed");
            return;
        }

        try {
            // Register connection
            SSEConnection connection = sseManager.registerConnection(exchange);

            // Set close listener
            exchange.addExchangeCompleteListener((ex, nextListener) -> {
                sseManager.unregisterConnection(connection);
                nextListener.proceed();
            });

            // Keep connection open (will be closed by client or server shutdown)
            // Don't call endExchange() - keep it persistent

        } catch (Exception e) {
            LOGGER.severe("Error handling SSE connection: " + e.getMessage());
            e.printStackTrace();
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Internal server error");
        }
    }
}
