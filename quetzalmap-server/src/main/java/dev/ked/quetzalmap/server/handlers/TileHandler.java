package dev.ked.quetzalmap.server.handlers;

import dev.ked.quetzalmap.web.tiles.Tile;
import dev.ked.quetzalmap.web.tiles.TileCoord;
import dev.ked.quetzalmap.web.tiles.TileManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP handler for serving map tiles.
 * Supports URLs like: /tiles/{world}/{zoom}/{x}_{z}.png
 */
public final class TileHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(TileHandler.class.getName());
    private static final Pattern TILE_PATTERN = Pattern.compile("^/tiles/([^/]+)/(\\d+)/([-\\d]+)_([-\\d]+)\\.png$");

    private final TileManager tileManager;
    private final Path tilesDirectory;
    private final Path worldsDirectory;

    public TileHandler(TileManager tileManager, Path tilesDirectory, Path worldsDirectory) {
        this.tileManager = tileManager;
        this.tilesDirectory = tilesDirectory;
        this.worldsDirectory = worldsDirectory;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String path = exchange.getRequestPath();
        Matcher matcher = TILE_PATTERN.matcher(path);

        if (!matcher.matches()) {
            sendError(exchange, StatusCodes.BAD_REQUEST, "Invalid tile path format");
            return;
        }

        try {
            String world = matcher.group(1);
            int zoom = Integer.parseInt(matcher.group(2));
            int x = Integer.parseInt(matcher.group(3));
            int z = Integer.parseInt(matcher.group(4));

            TileCoord coord = new TileCoord(world, zoom, x, z);
            LOGGER.fine("Tile request: " + coord);

            // Check if tile exists on disk
            Path tilePath = tilesDirectory.resolve(coord.getRelativePath());

            if (Files.exists(tilePath)) {
                LOGGER.fine("Serving cached tile: " + tilePath);
                // Serve from disk
                serveTileFromDisk(exchange, tilePath);
            } else {
                LOGGER.fine("Rendering new tile: " + coord);
                // Tile doesn't exist - need to render
                Path worldDir = worldsDirectory.resolve(coord.getWorld());
                renderAndServeTile(exchange, coord, worldDir);
            }

        } catch (NumberFormatException e) {
            sendError(exchange, StatusCodes.BAD_REQUEST, "Invalid tile coordinates");
        } catch (Exception e) {
            LOGGER.severe("Error serving tile: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Serve a tile from disk.
     * Fast path for cached tiles - still uses blocking I/O but tiles are small (~200KB).
     */
    private void serveTileFromDisk(HttpServerExchange exchange, Path tilePath) {
        try {
            // Add ETag for browser caching
            String etag = "\"" + tilePath.getFileName().toString() + "-" + Files.getLastModifiedTime(tilePath).toMillis() + "\"";
            exchange.getResponseHeaders().put(Headers.ETAG, etag);

            // Check If-None-Match for 304 Not Modified
            String ifNoneMatch = exchange.getRequestHeaders().getFirst(Headers.IF_NONE_MATCH);
            if (etag.equals(ifNoneMatch)) {
                exchange.setStatusCode(StatusCodes.NOT_MODIFIED);
                exchange.endExchange();
                return;
            }

            // Check if response already started (shouldn't happen, but be defensive)
            if (exchange.isResponseStarted()) {
                LOGGER.fine("Response already started for tile: " + tilePath);
                return;
            }

            // Read tile data (fast - tiles are small and likely in OS page cache)
            byte[] tileData = Files.readAllBytes(tilePath);

            // Set response headers BEFORE starting blocking mode
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "image/png");
            exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, tileData.length);
            exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "public, max-age=300"); // 5 min cache

            // Start blocking mode and send tile data
            exchange.startBlocking();
            exchange.getOutputStream().write(tileData);
            exchange.getOutputStream().close();

        } catch (IOException e) {
            LOGGER.warning("Failed to read tile from disk: " + tilePath);
            if (!exchange.isResponseStarted()) {
                sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Failed to read tile");
            }
        }
    }

    /**
     * Render a tile on-demand and serve it.
     * Uses proper async handling to avoid blocking HTTP worker threads.
     */
    private void renderAndServeTile(HttpServerExchange exchange, TileCoord coord, Path worldDir) {
        // Start async dispatch - HTTP thread can be released
        if (exchange.isInIoThread()) {
            exchange.dispatch(() -> renderAndServeTile(exchange, coord, worldDir));
            return;
        }

        try {
            LOGGER.fine("Rendering tile for world directory: " + worldDir.toAbsolutePath());

            // Render tile asynchronously - this returns immediately
            CompletableFuture<Tile> renderFuture = tileManager.renderTile(coord, worldDir);

            // When render completes, dispatch response back to IO thread
            renderFuture.whenCompleteAsync((tile, error) -> {
                if (error != null) {
                    LOGGER.severe("Failed to render tile " + coord + ": " + error.getMessage());
                    exchange.dispatch(() -> {
                        sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Tile render error");
                    });
                    return;
                }

                if (tile == null) {
                    exchange.dispatch(() -> {
                        sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Tile render returned null");
                    });
                    return;
                }

                // Tile rendered successfully - serve it
                Path tilePath = tilesDirectory.resolve(coord.getRelativePath());
                exchange.dispatch(() -> {
                    if (Files.exists(tilePath)) {
                        serveTileFromDisk(exchange, tilePath);
                    } else {
                        sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Tile file not found after render");
                    }
                });
            });

        } catch (Exception e) {
            LOGGER.severe("Error initiating tile render: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Render error");
        }
    }

    /**
     * Send an error response.
     */
    private void sendError(HttpServerExchange exchange, int statusCode, String message) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send(message);
    }
}
