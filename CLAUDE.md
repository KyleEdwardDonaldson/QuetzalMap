# QuetzalMap - Development Documentation

## Overview

QuetzalMap is a real-time web-based live map plugin for Minecraft Paper 1.21.3+ servers. It provides both a web interface and in-game map functionality for visualizing the Minecraft world, player positions, and plugin integrations.

## Current Implementation Status

### ✅ Completed Features

**Web Map System:**
- Real-time tile rendering from Minecraft world data (512×512 pixel tiles)
- Embedded Undertow web server on port 8123
- React + TypeScript + Leaflet.js frontend
- Server-Sent Events (SSE) for live updates
- Custom CRS (Coordinate Reference System) for proper Minecraft coordinate mapping
- Dynamic scale bar showing distance in blocks
- Multi-world support (Overworld, Nether, End)
- Smooth zoom/pan with tile caching

**Player Tracking:**
- Real-time player position updates via SSE
- Player markers using Minecraft character heads (Crafatar API)
- Live player list panel with join/leave events
- Click-to-center player location
- Player count indicator with real-time updates
- Player head avatars in player list

**Stormcraft Integration:**
- Real-time storm position tracking via SSE
- Storm markers with animated circles showing radius
- Storm movement indicators (direction arrows to target)
- Live storm list panel with detailed statistics
- Storm phase visualization (FORMING, PEAK, DISSIPATING)
- Storm type color coding (SHORT_WEAK, MEDIUM, LONG_DANGEROUS)
- Click-to-center storm location
- Storm count indicator with real-time updates
- Integration with Stormcraft's traveling storm system

**Performance Optimizations:**
- Caffeine caching for tile data
- Async tile rendering
- Batch chunk update processing
- SSE connection management
- Region file caching
- Clean logging (spam reduced to FINE level)

### 🚧 In Development

**In-Game Map:**
- Minimap using Minecraft map items
- Custom map renderer integration
- Marker system for in-game display

**Plugin Integrations (Remaining):**
- Bazaar: Shop location markers
- SilkRoad: Transporter and trade post markers
- StormcraftDungeons: Dungeon portal markers
- StormcraftEvents: Event location markers

## Architecture

### Multi-Module Maven Project

```
quetzalmap/
├─ quetzalmap-core/       # Platform-agnostic world parsing
│   ├─ Anvil region file parser (Querz NBT)
│   ├─ Chunk decompression (LZ4)
│   ├─ Color calculation for blocks
│   └─ World data abstractions
│
├─ quetzalmap-web/        # Tile rendering engine
│   ├─ TileRenderer - Converts chunks to 512×512 PNG tiles
│   ├─ TileManager - Caching and dirty tracking
│   ├─ ChunkPixelDataPool - Object pooling for performance
│   └─ TilePreGenerator - Background tile generation
│
├─ quetzalmap-server/     # Embedded web server
│   ├─ WebServer - Undertow HTTP server
│   ├─ TileHandler - Serves PNG tiles
│   ├─ SSEManager - Server-Sent Events broadcasting
│   ├─ SSEConnection - Individual SSE client connections
│   └─ WorldsHandler - World discovery API
│
├─ quetzalmap-paper/      # Paper plugin implementation
│   ├─ QuetzalMapPlugin - Main plugin class
│   ├─ PlayerTracker - Player position tracking
│   ├─ PlayerJoinListener - Broadcasts player join events
│   ├─ PlayerQuitListener - Broadcasts player leave events
│   ├─ ChunkEventListener - Tracks chunk changes
│   ├─ BatchUpdateScheduler - Debounces chunk updates
│   ├─ WorldAdapter - Bukkit world → file system bridge
│   └─ integration/
│       └─ StormcraftIntegration - Stormcraft storm tracking
│
├─ quetzalmap-ingame/     # In-game map features (planned)
│   └─ Custom map renderer for minimap
│
└─ quetzalmap-frontend/   # React web interface
    ├─ src/
    │   ├─ components/
    │   │   ├─ Map.tsx - Leaflet map container
    │   │   ├─ PlayerMarkers.tsx - Live player markers
    │   │   ├─ PlayerListPanel.tsx - Player list UI
    │   │   ├─ StormMarkers.tsx - Live storm markers
    │   │   ├─ StormListPanel.tsx - Storm list UI
    │   │   ├─ MapControls.tsx - UI controls
    │   │   └─ ScaleBar.tsx - Distance indicator
    │   └─ hooks/
    │       └─ useSSE.ts - SSE connection hook
    └─ Build: Vite + TypeScript
```

### Technology Stack

**Backend:**
- Java 21
- Paper API 1.21.3+
- Undertow (embedded web server)
- Caffeine (in-memory caching)
- Querz NBT (region file parsing)
- LZ4 Java (chunk decompression)

**Frontend:**
- TypeScript
- React 18
- Leaflet.js
- Vite (build tool)
- TailwindCSS

### Data Flow

```
1. Minecraft World (.mca region files)
   ↓
2. quetzalmap-core (RegionCache, chunk parsing)
   ↓
3. quetzalmap-web (TileRenderer → 512×512 PNG)
   ↓
4. quetzalmap-server (Undertow serves tiles via HTTP)
   ↓
5. quetzalmap-frontend (Leaflet displays tiles)

Real-time updates:
1. Player moves (Bukkit event)
   ↓
2. PlayerTracker (batches updates every 1 second)
   ↓
3. SSEManager (broadcasts to all connected clients)
   ↓
4. Frontend SSE hook (updates React state)
   ↓
5. PlayerMarkers component (moves marker on map)
```

## SSE Event Types

The plugin broadcasts these Server-Sent Events:

### `player_list`
Initial player list sent when client connects.
```json
{
  "players": [
    {
      "uuid": "...",
      "name": "PlayerName",
      "x": 100.5,
      "y": 64.0,
      "z": -200.3,
      "yaw": 90.0,
      "world": "world"
    }
  ]
}
```

### `player_join`
Broadcast when a player joins the server.
```json
{
  "uuid": "...",
  "name": "PlayerName",
  "x": 0.0,
  "y": 64.0,
  "z": 0.0,
  "yaw": 0.0,
  "world": "world"
}
```

### `player_moved`
Broadcast when a player moves >5 blocks (throttled to 1/sec per player).
```json
{
  "uuid": "...",
  "name": "PlayerName",
  "x": 105.2,
  "y": 65.0,
  "z": -195.8,
  "yaw": 135.0,
  "world": "world"
}
```

### `player_disconnect`
Broadcast when a player leaves.
```json
{
  "uuid": "...",
  "name": "PlayerName"
}
```

### `tile_update`
Broadcast when a chunk changes and tile is re-rendered.
```json
{
  "type": "tile_update",
  "world": "world",
  "zoom": 0,
  "x": 10,
  "z": -5
}
```

### `storm_update`
Broadcast every second with all active traveling storms (Stormcraft integration).
```json
{
  "storms": [
    {
      "id": "storm_1759634749624",
      "x": 5483.43,
      "z": -6299.97,
      "targetX": -11282.00,
      "targetZ": 12962.00,
      "radius": 2344.96,
      "baseRadius": 2344.96,
      "radiusMultiplier": 1.000,
      "phase": "DISSIPATING",
      "phaseSymbol": "§7⬇",
      "phaseMultiplier": 0.985,
      "type": "MEDIUM",
      "damage": 0.44,
      "speed": 2.00,
      "remainingSeconds": 1342,
      "world": "world"
    }
  ]
}
```

## Coordinate System

**Minecraft Coordinates → Leaflet Coordinates:**
- Minecraft X → Leaflet lng (longitude)
- Minecraft Z → Leaflet lat (latitude, negated)
- Leaflet position: `[-z, x]`

**Tile Coordinates:**
- Each tile = 512×512 pixels = 512×512 blocks (at zoom 0)
- Tile files named: `{x}_{z}.png`
- Tile coordinates map directly to Minecraft region file coordinates
- Example: `2656_-2158.png` = region file `r.2656.-2158.mca`

**Custom CRS (Map.tsx):**
```typescript
const QuetzalCRS = L.extend({}, L.CRS.Simple, {
  transformation: new L.Transformation(1, 0, -1, 0),
  scale: (zoom) => Math.pow(2, zoom),
  zoom: (scale) => Math.log(scale) / Math.LN2
});
```

## Performance Characteristics

**Tile Rendering:**
- First render: ~50-100ms per 512×512 tile
- Cached tiles: <1ms (served from disk)
- Region cache: Significant speedup (avoids repeated NBT parsing)

**Web Server:**
- Undertow: Low overhead, handles 50+ concurrent connections
- SSE keepalive: ~2KB/s per connected client
- Tile serving: Cached with ETag/If-None-Match (304 Not Modified)

**Memory Usage:**
- Base plugin: ~100-200MB
- Tile cache: ~500MB for 10,000 tiles
- Region cache: ~50MB per cached region
- Per-connection SSE: ~10KB

**Update Batching:**
- Chunk updates: Batched every 100ms (2 ticks)
- Player movement: Throttled to 1/sec per player, >5 block threshold
- SSE broadcasts: Immediate (no batching)

## Configuration

**Plugin Config** (`plugins/QuetzalMap/config.yml`):
```yaml
# Web server settings
server:
  host: "0.0.0.0"
  port: 8123

# Rendering settings
rendering:
  tile-size: 512
  zoom-levels: 1

# World settings
worlds:
  - world
  - world_nether
  - world_the_end

# Map display
map:
  default-zoom: 0
  min-zoom: -3
  max-zoom: 3
```

**Frontend Config** (`.env`):
```env
VITE_API_URL=http://your-server-ip:8123
```

## Development Workflow

### Building

```bash
# Build backend
cd /var/repos/QuetzalMap
mvn clean package

# Deploy to server
cp quetzalmap-paper/target/QuetzalMap-*.jar \
   /var/lib/pterodactyl/volumes/{server-id}/plugins/

# Build frontend
cd quetzalmap-frontend
npm install
npm run build

# Serve frontend (development)
python3 -m http.server 7825 --bind 0.0.0.0 --directory dist
```

### Testing

**Backend:**
1. Start Paper server with QuetzalMap installed
2. Check logs for "QuetzalMap enabled successfully!"
3. Verify web server: `http://localhost:8123`
4. Test SSE: `curl http://localhost:8123/events`

**Frontend:**
1. Build frontend: `npm run build`
2. Serve: `python3 -m http.server 7825 --directory dist`
3. Open browser: `http://localhost:7825`
4. Check console for SSE connection
5. Verify tiles load and player markers appear

### Debugging

**Common Issues:**

**Tiles not loading:**
- Check `plugins/QuetzalMap/tiles/` exists
- Verify world directory is correct
- Check server logs for render errors
- Ensure port 8123 is accessible

**SSE not connecting:**
- Check browser console for CORS errors
- Verify SSEManager is initialized
- Check firewall allows port 8123
- Test with: `curl http://server:8123/events`

**Player markers not showing:**
- Check PlayerTracker is running (logs every 1 sec)
- Verify player UUID format
- Check Crafatar API is accessible
- Test: `https://crafatar.com/avatars/{uuid}`

**Storm markers not showing:**
- Verify Stormcraft plugin is installed and enabled
- Check StormcraftIntegration initialized in logs
- Verify `storm_update` event type is in useSSE.ts event list
- Test SSE: `curl http://server:8123/events | grep storm_update`
- Check browser console for `[SSE] Event: storm_update` logs
- Ensure traveling storms are active (not just stationary storms)

**Coordinate issues:**
- Verify CRS transformation is correct
- Check player position uses `[-z, x]`
- Ensure tile coordinates match region files

## Plugin Integration Points (Planned)

### API Design

```java
public interface QuetzalMapAPI {
    /**
     * Register a custom marker type
     */
    void registerMarkerType(String id, MarkerType type);

    /**
     * Add a marker to the map
     */
    void addMarker(Marker marker);

    /**
     * Remove a marker
     */
    void removeMarker(String markerId);

    /**
     * Broadcast a custom update
     */
    void broadcastUpdate(String eventType, String jsonData);
}
```

### Integration Examples

**Stormcraft:**
```java
// Listen for storm start event
@EventHandler
public void onStormStart(StormcraftStormStartEvent event) {
    Storm storm = event.getStorm();
    Location center = storm.getCenter();

    // Broadcast to web map
    quetzalMapAPI.broadcastUpdate("storm_start",
        String.format("{\"x\":%.2f,\"z\":%.2f,\"radius\":%d}",
            center.getX(), center.getZ(), storm.getRadius()));
}
```

## Future Enhancements

### Planned Features

**Short-term (1-2 weeks):**
- Multiple zoom levels (downsampled tiles)
- Chunk update notifications (real-time tile refresh)
- Storm markers (Stormcraft integration)
- Shop markers (Bazaar integration)

**Medium-term (1-2 months):**
- In-game minimap using Minecraft map items
- WorldGuard region overlays
- Towny town boundaries
- Custom waypoint system

**Long-term (3+ months):**
- 3D terrain rendering
- Historical storm path playback
- Multi-server map aggregation
- Mobile app companion

## Maintenance

### Log Levels

All verbose logs are set to `LOGGER.fine()` to reduce spam:
- SSE connection events
- Player tracking updates
- Tile rendering progress
- Batch update processing

To enable verbose logging:
```yaml
# bukkit.yml or paper-global.yml
logging:
  level: FINE
```

### Performance Monitoring

Watch for:
- TPS drops when rendering many tiles
- Memory growth from region cache
- SSE connection leaks
- Excessive tile re-rendering

### Updating

When updating the plugin:
1. Build new JAR: `mvn clean package`
2. Stop server or use `/reload confirm`
3. Replace JAR in plugins folder
4. Restart server
5. Verify web server starts on port 8123
6. Test SSE connection and tile loading

## Credits

- **Querz NBT Library** - Region file parsing
- **Leaflet.js** - Interactive map library
- **Undertow** - Embedded web server
- **Caffeine** - High-performance caching
- **Crafatar** - Minecraft avatar API

## License

MIT License - See LICENSE file for details

---

**Developed by Kyle Edward Donaldson for Quetzal's Stormcraft Server**
