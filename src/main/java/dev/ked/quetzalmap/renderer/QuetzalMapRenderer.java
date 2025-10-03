package dev.ked.quetzalmap.renderer;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.model.Marker;
import dev.ked.quetzalmap.model.MarkerType;
import dev.ked.quetzalmap.model.PlayerMapData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.List;

public class QuetzalMapRenderer extends MapRenderer {

    private final QuetzalMapPlugin plugin;
    private final Player player;
    private boolean initialized = false;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    public QuetzalMapRenderer(QuetzalMapPlugin plugin, Player player) {
        super(false); // non-contextual - render every frame but don't interfere with terrain
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        // Non-contextual rendering - we're an overlay on top of the default terrain
        // The default renderer handles terrain discovery, we just add our markers

        // Render overlays (storms) on top of terrain
        renderOverlays(map, canvas, player);

        // Always update markers/cursors as they need to be live
        renderMarkers(map, canvas, player);
    }

    private void renderMarkers(MapView map, MapCanvas canvas, Player player) {
        MapCursorCollection cursors = new MapCursorCollection();

        PlayerMapData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        // Don't add player cursor - Minecraft adds it automatically with setTrackingPosition(true)

        // Get all markers from marker manager
        List<Marker> markers = plugin.getMarkerManager().getAllMarkers();

        int cursorCount = 0; // Start at 0, no manual player cursor
        int maxCursors = plugin.getConfig().getInt("performance.max-markers-per-map", 500);

        for (Marker marker : markers) {
            if (cursorCount >= maxCursors) {
                break;
            }

            // Check if player has this marker type visible
            if (!playerData.isMarkerVisible(marker.getType())) {
                continue;
            }

            // Check if marker is in the same world
            if (!marker.getLocation().getWorld().equals(player.getWorld())) {
                continue;
            }

            // Calculate position relative to map center
            int markerX = marker.getLocation().getBlockX() - map.getCenterX();
            int markerZ = marker.getLocation().getBlockZ() - map.getCenterZ();

            // Scale to map coordinates (-128 to 127)
            double scale = 128.0 / (map.getScale().getValue() * 128.0);
            int x = (int) (markerX * scale);
            int z = (int) (markerZ * scale);

            // Only render if within map bounds
            if (Math.abs(x) <= 127 && Math.abs(z) <= 127) {
                cursors.addCursor(
                    (byte) Math.max(-128, Math.min(127, x)),
                    (byte) Math.max(-128, Math.min(127, z)),
                    (byte) 8, // Direction (not used for most cursor types)
                    marker.getCursorType().getValue(),
                    true,
                    marker.getName()
                );
                cursorCount++;
            }
        }

        // Set the new cursor collection
        canvas.setCursors(cursors);
    }

    private void renderOverlays(MapView map, MapCanvas canvas, Player player) {
        PlayerMapData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        List<Marker> markers = plugin.getMarkerManager().getAllMarkers();

        for (Marker marker : markers) {
            // Check if player has this marker type visible
            if (!playerData.isMarkerVisible(marker.getType())) {
                continue;
            }

            // Check if marker is in the same world
            if (!marker.getLocation().getWorld().equals(player.getWorld())) {
                continue;
            }

            // Render based on marker type
            if (marker.getType().equals(MarkerType.STORM.getConfigKey())) {
                renderStormOverlay(map, canvas, player, marker);
            }
        }
    }

    private void renderStormOverlay(MapView map, MapCanvas canvas, Player player, Marker marker) {
        if (!marker.hasData("radius")) {
            return;
        }

        double radius = marker.getData("radius", Double.class);
        Location stormLoc = marker.getLocation();

        // Calculate storm position on map
        double scale = 128.0 / (map.getScale().getValue() * 128.0);
        int stormX = (int) ((stormLoc.getX() - map.getCenterX()) * scale) + 64;
        int stormZ = (int) ((stormLoc.getZ() - map.getCenterZ()) * scale) + 64;

        // Calculate radius in map pixels
        int radiusPixels = (int) (radius * scale);

        // Get storm color and opacity from config
        String colorHex = plugin.getConfig().getString("markers.storms.overlay-color", "#FF0000");
        double opacity = plugin.getConfig().getDouble("markers.storms.overlay-opacity", 0.3);

        // Parse color and apply opacity
        Color stormColor = parseHexColor(colorHex);
        int alpha = (int) (opacity * 255);
        stormColor = new Color(stormColor.getRed(), stormColor.getGreen(), stormColor.getBlue(), alpha);
        byte mapColor = MapPalette.matchColor(stormColor);

        // Draw semi-transparent filled circle for storm radius
        drawFilledCircleTransparent(canvas, stormX, stormZ, radiusPixels, mapColor, opacity);

        // Draw unfilled circle border for storm edge
        String borderColorHex = plugin.getConfig().getString("markers.storms.border-color", "#FF0000");
        Color borderColor = parseHexColor(borderColorHex);
        byte borderMapColor = MapPalette.matchColor(borderColor);
        int borderThickness = plugin.getConfig().getInt("markers.storms.border-thickness", 2);
        drawCircleOutline(canvas, stormX, stormZ, radiusPixels, borderMapColor, borderThickness);

        // Draw direction arrow if target data exists
        if (marker.hasData("targetX") && marker.hasData("targetZ")) {
            double targetX = marker.getData("targetX", Double.class);
            double targetZ = marker.getData("targetZ", Double.class);

            // Calculate direction vector
            double dx = targetX - stormLoc.getX();
            double dz = targetZ - stormLoc.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance > 1.0) { // Only draw arrow if actually moving
                // Normalize direction
                dx /= distance;
                dz /= distance;

                // Draw arrow (20 pixels long from storm center)
                int arrowLength = 20;
                int arrowEndX = stormX + (int) (dx * arrowLength);
                int arrowEndZ = stormZ + (int) (dz * arrowLength);

                // Get arrow color and opacity from config
                String arrowColorHex = plugin.getConfig().getString("markers.storms.direction-arrow-color", "#FFFF00");
                double arrowOpacity = plugin.getConfig().getDouble("markers.storms.arrow-opacity", 0.8);
                Color arrowCol = parseHexColor(arrowColorHex);
                int arrowAlpha = (int) (arrowOpacity * 255);
                arrowCol = new Color(arrowCol.getRed(), arrowCol.getGreen(), arrowCol.getBlue(), arrowAlpha);
                byte arrowColor = MapPalette.matchColor(arrowCol);
                drawArrow(canvas, stormX, stormZ, arrowEndX, arrowEndZ, arrowColor);
            }
        }

        // Draw distance text if enabled
        if (plugin.getConfig().getBoolean("markers.storms.show-distance", true)) {
            double distanceToPlayer = player.getLocation().distance(stormLoc);
            int distanceBlocks = (int) distanceToPlayer;

            if (stormX >= 0 && stormX < 128 && stormZ >= 0 && stormZ < 128) {
                String distText = distanceBlocks + "b";
                drawText(canvas, stormX - 6, stormZ + radiusPixels + 4, distText, MapPalette.matchColor(Color.WHITE));
            }
        }
    }

    private void drawFilledCircle(MapCanvas canvas, int centerX, int centerZ, int radius, byte color) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    int px = centerX + x;
                    int pz = centerZ + z;
                    if (px >= 0 && px < 128 && pz >= 0 && pz < 128) {
                        canvas.setPixel(px, pz, color);
                    }
                }
            }
        }
    }

    private void drawCircleOutline(MapCanvas canvas, int centerX, int centerZ, int radius, byte color, int thickness) {
        // Use midpoint circle algorithm for smoother circles
        for (int t = 0; t < thickness; t++) {
            int r = radius - t;
            if (r <= 0) break;

            int x = r;
            int z = 0;
            int err = 0;

            while (x >= z) {
                // Draw 8 octants
                setPixelSafe(canvas, centerX + x, centerZ + z, color);
                setPixelSafe(canvas, centerX + z, centerZ + x, color);
                setPixelSafe(canvas, centerX - z, centerZ + x, color);
                setPixelSafe(canvas, centerX - x, centerZ + z, color);
                setPixelSafe(canvas, centerX - x, centerZ - z, color);
                setPixelSafe(canvas, centerX - z, centerZ - x, color);
                setPixelSafe(canvas, centerX + z, centerZ - x, color);
                setPixelSafe(canvas, centerX + x, centerZ - z, color);

                if (err <= 0) {
                    z += 1;
                    err += 2 * z + 1;
                }
                if (err > 0) {
                    x -= 1;
                    err -= 2 * x + 1;
                }
            }
        }
    }

    private void setPixelSafe(MapCanvas canvas, int x, int z, byte color) {
        if (x >= 0 && x < 128 && z >= 0 && z < 128) {
            canvas.setPixel(x, z, color);
        }
    }

    private void drawFilledCircleTransparent(MapCanvas canvas, int centerX, int centerZ, int radius, byte color, double opacity) {
        // For semi-transparent effect, we can use a dithering pattern
        // The pattern density is based on the opacity value
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    int px = centerX + x;
                    int pz = centerZ + z;
                    if (px >= 0 && px < 128 && pz >= 0 && pz < 128) {
                        // Use a checkerboard pattern for transparency effect
                        // The pattern density depends on opacity
                        boolean shouldDraw;
                        if (opacity < 0.25) {
                            // Very transparent - only draw every 4th pixel
                            shouldDraw = ((px + pz) % 4 == 0);
                        } else if (opacity < 0.5) {
                            // Semi-transparent - checkerboard pattern
                            shouldDraw = ((px + pz) % 2 == 0);
                        } else if (opacity < 0.75) {
                            // Mostly opaque - skip every 4th pixel
                            shouldDraw = !((px + pz) % 4 == 0);
                        } else {
                            // Nearly solid
                            shouldDraw = true;
                        }

                        if (shouldDraw) {
                            canvas.setPixel(px, pz, color);
                        }
                    }
                }
            }
        }
    }

    private Color parseHexColor(String hex) {
        try {
            // Remove # if present
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }

            // Parse RGB
            if (hex.length() >= 6) {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                return new Color(r, g, b);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid color format: " + hex);
        }
        return new Color(255, 0, 0); // Default to red
    }

    private void drawRectangleOutline(MapCanvas canvas, int x1, int z1, int x2, int z2, byte color, int thickness) {
        // Ensure coordinates are ordered correctly
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        // Draw horizontal lines (top and bottom)
        for (int t = 0; t < thickness; t++) {
            for (int x = minX; x <= maxX; x++) {
                // Top line
                if (minZ + t >= 0 && minZ + t < 128 && x >= 0 && x < 128) {
                    canvas.setPixel(x, minZ + t, color);
                }
                // Bottom line
                if (maxZ - t >= 0 && maxZ - t < 128 && x >= 0 && x < 128) {
                    canvas.setPixel(x, maxZ - t, color);
                }
            }
        }

        // Draw vertical lines (left and right)
        for (int t = 0; t < thickness; t++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Left line
                if (minX + t >= 0 && minX + t < 128 && z >= 0 && z < 128) {
                    canvas.setPixel(minX + t, z, color);
                }
                // Right line
                if (maxX - t >= 0 && maxX - t < 128 && z >= 0 && z < 128) {
                    canvas.setPixel(maxX - t, z, color);
                }
            }
        }
    }

    private void drawArrow(MapCanvas canvas, int x1, int z1, int x2, int z2, byte color) {
        // Draw main line
        drawLine(canvas, x1, z1, x2, z2, color);

        // Calculate arrow head
        double angle = Math.atan2(z2 - z1, x2 - x1);
        int arrowSize = 4;

        // Left arrowhead point
        int leftX = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int leftZ = (int) (z2 - arrowSize * Math.sin(angle - Math.PI / 6));
        drawLine(canvas, x2, z2, leftX, leftZ, color);

        // Right arrowhead point
        int rightX = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int rightZ = (int) (z2 - arrowSize * Math.sin(angle + Math.PI / 6));
        drawLine(canvas, x2, z2, rightX, rightZ, color);
    }

    private void drawLine(MapCanvas canvas, int x1, int z1, int x2, int z2, byte color) {
        // Bresenham's line algorithm
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;
        int err = dx - dz;

        while (true) {
            if (x1 >= 0 && x1 < 128 && z1 >= 0 && z1 < 128) {
                canvas.setPixel(x1, z1, color);
            }

            if (x1 == x2 && z1 == z2) break;

            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                z1 += sz;
            }
        }
    }

    private void drawText(MapCanvas canvas, int x, int z, String text, byte color) {
        // Draw text using simple pixel font (since MapFont API varies by version)
        // For now, skip text rendering - focus on visual overlays
        // TODO: Implement custom pixel font for distance labels
    }
}
