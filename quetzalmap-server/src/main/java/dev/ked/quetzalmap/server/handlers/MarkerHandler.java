package dev.ked.quetzalmap.server.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.util.logging.Logger;

/**
 * HTTP handler for marker API.
 * Returns JSON data for map markers (storms, shops, etc.).
 */
public final class MarkerHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(MarkerHandler.class.getName());

    public MarkerHandler() {
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
            // TODO: Implement marker data collection from integrations
            // For now, return empty marker set

            String json = buildMarkersJson();

            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(json);

        } catch (Exception e) {
            LOGGER.severe("Error handling markers request: " + e.getMessage());
            e.printStackTrace();
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Internal server error");
        }
    }

    /**
     * Build markers JSON response.
     * TODO: Integrate with Stormcraft, BetterShop, etc.
     */
    private String buildMarkersJson() {
        return """
                {
                  "markers": {
                    "storms": [],
                    "shops": [],
                    "transporters": [],
                    "events": [],
                    "dungeons": []
                  }
                }
                """;
    }
}
