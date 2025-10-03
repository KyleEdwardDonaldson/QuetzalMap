package dev.ked.quetzalmap.server;

import dev.ked.quetzalmap.server.handlers.SSEHandler;
import dev.ked.quetzalmap.server.handlers.TileHandler;
import dev.ked.quetzalmap.server.handlers.MarkerHandler;
import dev.ked.quetzalmap.server.handlers.WorldsHandler;
import dev.ked.quetzalmap.server.sse.SSEManager;
import dev.ked.quetzalmap.web.tiles.TileManager;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Embedded HTTP server for serving map tiles and handling real-time updates.
 * Uses Undertow for high-performance HTTP/SSE.
 */
public final class WebServer {
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

    private final String host;
    private final int port;
    private final TileManager tileManager;
    private final Path tilesDirectory;
    private final Path worldsDirectory;
    private final SSEManager sseManager;
    private Undertow server;

    public WebServer(String host, int port, TileManager tileManager, Path tilesDirectory, Path worldsDirectory) {
        this.host = host;
        this.port = port;
        this.tileManager = tileManager;
        this.tilesDirectory = tilesDirectory;
        this.worldsDirectory = worldsDirectory;
        this.sseManager = new SSEManager();
    }

    /**
     * Start the web server.
     */
    public void start() {
        if (server != null) {
            LOGGER.warning("WebServer is already running");
            return;
        }

        try {
            LOGGER.info("Starting WebServer on " + host + ":" + port + "...");

            // Create handlers
            TileHandler tileHandler = new TileHandler(tileManager, tilesDirectory, worldsDirectory);
            SSEHandler sseHandler = new SSEHandler(sseManager);
            MarkerHandler markerHandler = new MarkerHandler();
            WorldsHandler worldsHandler = new WorldsHandler(worldsDirectory);
            LOGGER.info("Handlers created successfully");

            // Build path handler
            PathHandler pathHandler = new PathHandler()
                    .addPrefixPath("/tiles", tileHandler)
                    .addPrefixPath("/events", sseHandler)
                    .addPrefixPath("/api/markers", markerHandler)
                    .addExactPath("/api/worlds", worldsHandler)
                    .addExactPath("/", this::handleRoot)
                    .addExactPath("/health", this::handleHealth);
            LOGGER.info("Path handlers registered");

            // Wrap with CORS handler
            HttpHandler corsHandler = addCorsHeaders(pathHandler);
            LOGGER.info("CORS handler added");

            // Wrap with gzip compression
            HttpHandler compressedHandler = addCompressionHandler(corsHandler);
            LOGGER.info("Compression handler added");

            // Build server
            LOGGER.info("Building Undertow server on " + host + ":" + port);
            server = Undertow.builder()
                    .addHttpListener(port, host)
                    .setHandler(compressedHandler)
                    .build();
            LOGGER.info("Undertow server built, calling start()...");

            server.start();
            LOGGER.info("Undertow server.start() completed");

            // Get actual bound address info
            var listenerInfo = server.getListenerInfo();
            LOGGER.info("Undertow listener info: " + listenerInfo);

            LOGGER.info(String.format("WebServer started successfully on http://%s:%d", host, port));
            LOGGER.info("  - Tiles:   http://" + host + ":" + port + "/tiles/{world}/{zoom}/{x}_{z}.png");
            LOGGER.info("  - Events:  http://" + host + ":" + port + "/events");
            LOGGER.info("  - Markers: http://" + host + ":" + port + "/api/markers");
            LOGGER.info("  - Worlds:  http://" + host + ":" + port + "/api/worlds");
            LOGGER.info("  - Health:  http://" + host + ":" + port + "/health");

            // Attempt to verify the server is actually listening
            try {
                java.net.Socket testSocket = new java.net.Socket();
                testSocket.connect(new java.net.InetSocketAddress(host.equals("0.0.0.0") ? "127.0.0.1" : host, port), 1000);
                testSocket.close();
                LOGGER.info("✓ Verified: Server is accepting connections on port " + port);
            } catch (Exception e) {
                LOGGER.severe("✗ WARNING: Server claims to be running but cannot accept connections!");
                LOGGER.severe("  This may indicate a network configuration issue.");
                LOGGER.severe("  Error: " + e.getMessage());
            }

        } catch (Exception e) {
            LOGGER.severe("Failed to start WebServer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("WebServer startup failed", e);
        }
    }

    /**
     * Stop the web server.
     */
    public void stop() {
        if (server == null) {
            return;
        }

        LOGGER.info("Stopping WebServer...");

        try {
            // Close all SSE connections
            sseManager.shutdown();

            // Stop server
            server.stop();
            server = null;

            LOGGER.info("WebServer stopped");

        } catch (Exception e) {
            LOGGER.severe("Error stopping WebServer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the SSE manager for broadcasting updates.
     */
    public SSEManager getSSEManager() {
        return sseManager;
    }

    /**
     * Add CORS headers to allow frontend access.
     */
    private HttpHandler addCorsHeaders(HttpHandler next) {
        return exchange -> {
            exchange.getResponseHeaders().put(io.undertow.util.HttpString.tryFromString("Access-Control-Allow-Origin"), "*");
            exchange.getResponseHeaders().put(io.undertow.util.HttpString.tryFromString("Access-Control-Allow-Methods"), "GET, POST, OPTIONS");
            exchange.getResponseHeaders().put(io.undertow.util.HttpString.tryFromString("Access-Control-Allow-Headers"), "Content-Type");

            // Handle preflight OPTIONS request
            if (exchange.getRequestMethod().equals(Methods.OPTIONS)) {
                exchange.setStatusCode(204);
                exchange.endExchange();
                return;
            }

            next.handleRequest(exchange);
        };
    }

    /**
     * Add gzip compression for responses.
     */
    private HttpHandler addCompressionHandler(HttpHandler next) {
        ContentEncodingRepository repository = new ContentEncodingRepository()
                .addEncodingHandler("gzip", new GzipEncodingProvider(), 50);
        return new EncodingHandler(repository).setNext(next);
    }

    /**
     * Handle root path.
     */
    private void handleRoot(HttpServerExchange exchange) {
        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("QuetzalMap Web Server\n\nEndpoints:\n" +
                "  GET /tiles/{world}/{zoom}/{x}_{z}.png - Map tiles\n" +
                "  GET /events - Server-Sent Events\n" +
                "  GET /api/markers - Marker data\n" +
                "  GET /api/worlds - Available worlds\n" +
                "  GET /health - Health check\n");
    }

    /**
     * Handle health check.
     */
    private void handleHealth(HttpServerExchange exchange) {
        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(String.format(
                "{\"status\":\"ok\",\"connections\":%d,\"cache\":%d,\"dirty\":%d}",
                sseManager.getConnectionCount(),
                tileManager.getCacheStats().hotSize() + tileManager.getCacheStats().warmSize(),
                tileManager.getDirtyCount()
        ));
    }

    /**
     * Get server status.
     */
    public boolean isRunning() {
        return server != null;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
