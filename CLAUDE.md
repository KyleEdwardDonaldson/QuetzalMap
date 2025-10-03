# QuetzalMap - Custom Minimap Plugin Implementation Plan

## Overview
QuetzalMap is a custom minimap plugin for the Quetzal server that provides players with a large-area map view using Minecraft's native map items. The plugin is designed to integrate seamlessly with all Quetzal server plugins to display real-time information about storms, shops, transporters, dungeons, and events.

## Core Features

### 1. Map System
- **Large Area Coverage**: Display a significantly larger area than vanilla maps (configurable radius)
- **Auto-Update**: Real-time map updates as players move
- **Player Tracking**: Show player position and direction on the map
- **Spawn Item**: All players receive a map item on join (configurable)
- **Multi-World Support**: Different maps for different worlds

### 2. Map Rendering
- **Base Terrain**: Render the world terrain using Minecraft's native map rendering
- **Custom Overlays**: Layer system for adding markers and icons on top of terrain
- **Icon System**: Custom pixel art icons for different marker types
- **Color Coding**: Use map colors to differentiate marker types
- **Update Rate**: Configurable update frequency (default: 1 tick for smooth updates)

### 3. Marker Types
All markers should be toggleable via player preferences:
- **Storms**: Active storm positions with direction indicators
- **Shops**: Bazaar locations
- **Transporters**: SilkRoad transporter stations
- **Trade Posts**: SilkRoad trade post locations
- **Dungeons**: StormcraftDungeons entry points
- **Events**: StormcraftEvents active event locations
- **Custom POIs**: Admin-defined points of interest

## Plugin Integrations

### Stormcraft Integration
**Purpose**: Display active storms on the map with movement indicators

**Integration Points**:
- Hook into Stormcraft's storm tracking system
- Get storm center coordinates
- Get storm radius/size
- Get storm movement vector (direction and speed)
- Get storm type (for different visual representations)

**Map Display**:
- Storm center marked with unique icon
- Storm radius shown as colored circle/area
- Movement arrow indicating storm direction
- Color-coded by storm severity/type

**API Requirements**:
```java
// Need from Stormcraft:
- List<Storm> getActiveStorms()
- Location getStormCenter(Storm storm)
- double getStormRadius(Storm storm)
- Vector getStormVelocity(Storm storm)
- StormType getStormType(Storm storm)
```

### Bazaar Integration
**Purpose**: Show shop locations on the map

**Integration Points**:
- Get all active shop locations
- Get shop owner/name
- Get shop type (if applicable)
- Listen for shop creation/deletion events

**Map Display**:
- Shop icon at shop location
- Different colors for different shop types
- Clickable for more info (if hovering supported)

**API Requirements**:
```java
// Need from Bazaar:
- List<Shop> getAllShops()
- Location getShopLocation(Shop shop)
- String getShopName(Shop shop)
- UUID getShopOwner(Shop shop)
```

### SilkRoad Integration
**Purpose**: Display transporters and trade posts

**Integration Points**:
- Get all transporter station locations
- Get all trade post locations
- Get transporter network connections
- Get trade post status (active/inactive)

**Map Display**:
- Transporter stations with unique icon
- Trade posts with unique icon
- Optional: Lines connecting transporter network routes
- Color coding for trade post status

**API Requirements**:
```java
// Need from SilkRoad:
- List<Transporter> getAllTransporters()
- List<TradePost> getAllTradePosts()
- Location getTransporterLocation(Transporter t)
- Location getTradePostLocation(TradePost tp)
- boolean isTradePostActive(TradePost tp)
```

### StormcraftEvents Integration
**Purpose**: Show active server events on the map

**Integration Points**:
- Get active event locations
- Get event type
- Get event duration/time remaining
- Listen for event start/end

**Map Display**:
- Event location with unique icon
- Different colors/icons per event type
- Pulsing/animated effect for active events

**API Requirements**:
```java
// Need from StormcraftEvents:
- List<Event> getActiveEvents()
- Location getEventLocation(Event event)
- EventType getEventType(Event event)
- long getTimeRemaining(Event event)
```

### StormcraftDungeons Integration
**Purpose**: Display dungeon entrance locations

**Integration Points**:
- Get all dungeon entrance locations
- Get dungeon name/type
- Get dungeon difficulty
- Get dungeon status (available/in-use)

**Map Display**:
- Dungeon entrance icon
- Color-coded by difficulty
- Visual indicator if dungeon is occupied

**API Requirements**:
```java
// Need from StormcraftDungeons:
- List<Dungeon> getAllDungeons()
- Location getDungeonEntrance(Dungeon dungeon)
- String getDungeonName(Dungeon dungeon)
- DungeonDifficulty getDifficulty(Dungeon dungeon)
- boolean isDungeonOccupied(Dungeon dungeon)
```

## Technical Architecture

### Core Components

#### 1. MapManager
- Manages map instances for all online players
- Handles map creation and distribution
- Coordinates update cycles
- Manages map data caching

#### 2. MapRenderer (extends MapRenderer)
- Custom Bukkit MapRenderer implementation
- Renders base terrain
- Renders overlay markers
- Handles player position updates

#### 3. MarkerManager
- Centralized marker registration system
- Manages all marker types and positions
- Handles marker visibility toggles
- Provides marker update events

#### 4. IntegrationManager
- Manages soft dependencies on other plugins
- Provides abstraction layer for plugin integrations
- Handles graceful degradation if plugins not present
- Registers event listeners for integrated plugins

#### 5. PlayerMapData
- Per-player map configuration
- Marker visibility preferences
- Map zoom level
- Custom waypoints

#### 6. IconRegistry
- Stores pixel art icons for all marker types
- Provides icon rendering utilities
- Handles icon scaling and rotation

### Configuration Structure

```yaml
quetzalmap:
  # Map Settings
  map:
    auto-give-on-join: true
    map-radius: 1024  # blocks from center
    update-rate: 1  # ticks between updates
    zoom-levels:
      - 512
      - 1024
      - 2048
      - 4096

  # Marker Settings
  markers:
    storms:
      enabled: true
      show-direction: true
      show-radius: true
      color: RED
    shops:
      enabled: true
      color: GREEN
    transporters:
      enabled: true
      color: BLUE
    trade-posts:
      enabled: true
      color: YELLOW
    events:
      enabled: true
      color: PURPLE
    dungeons:
      enabled: true
      color: DARK_RED

  # Integration Settings
  integrations:
    stormcraft:
      enabled: true
      update-interval: 20  # ticks
    bazaar:
      enabled: true
      max-shops-displayed: 100
    silkroad:
      enabled: true
    stormcraft-events:
      enabled: true
    stormcraft-dungeons:
      enabled: true

  # Performance Settings
  performance:
    async-rendering: true
    cache-terrain: true
    max-markers-per-map: 500
```

## Implementation Phases

### Phase 1: Core Map System
1. Create plugin structure and build configuration
2. Implement MapManager and basic map distribution
3. Create custom MapRenderer for terrain rendering
4. Implement player position tracking and display
5. Add basic map item handling (prevent dropping, etc.)

### Phase 2: Marker System
1. Implement MarkerManager with registration API
2. Create IconRegistry and basic icon rendering
3. Implement marker overlay rendering on maps
4. Add marker visibility toggles
5. Create player preferences system

### Phase 3: Stormcraft Integration
1. Create Stormcraft integration module
2. Hook into storm tracking system
3. Implement storm marker rendering
4. Add storm direction indicators
5. Add storm radius visualization

### Phase 4: Shop & Economy Integrations
1. Create Bazaar integration module
2. Implement shop marker system
3. Create SilkRoad integration module
4. Add transporter markers
5. Add trade post markers

### Phase 5: Event & Dungeon Integrations
1. Create StormcraftEvents integration module
2. Implement event markers with animations
3. Create StormcraftDungeons integration module
4. Add dungeon entrance markers
5. Implement difficulty-based color coding

### Phase 6: Polish & Optimization
1. Performance optimization (async rendering, caching)
2. Add player commands (/map toggle, /map zoom, etc.)
3. Implement permission system
4. Add configuration reload support
5. Create comprehensive documentation

## Commands & Permissions

### Commands
- `/qmap` - Main command
  - `/qmap toggle <marker-type>` - Toggle marker visibility
  - `/qmap zoom <level>` - Change map zoom level
  - `/qmap reload` - Reload configuration (admin)
  - `/qmap give <player>` - Give map to player (admin)
  - `/qmap info` - Show map info and active markers

### Permissions
- `quetzalmap.use` - Basic map usage (default: true)
- `quetzalmap.toggle` - Toggle marker types
- `quetzalmap.zoom` - Change zoom levels
- `quetzalmap.admin` - Admin commands (reload, give)
- `quetzalmap.markers.*` - See all marker types
  - `quetzalmap.markers.storms`
  - `quetzalmap.markers.shops`
  - `quetzalmap.markers.transporters`
  - `quetzalmap.markers.events`
  - `quetzalmap.markers.dungeons`

## API for Other Plugins

```java
public interface QuetzalMapAPI {
    /**
     * Register a custom marker type
     */
    void registerMarkerType(String id, MarkerType type);

    /**
     * Add a marker to all maps
     */
    void addMarker(Marker marker);

    /**
     * Remove a marker from all maps
     */
    void removeMarker(String markerId);

    /**
     * Update marker position
     */
    void updateMarker(String markerId, Location newLocation);

    /**
     * Get all active markers of a type
     */
    List<Marker> getMarkers(String type);
}
```

## Dependencies

### Required
- Paper API 1.21.3+
- Java 21+

### Soft Dependencies (Integrations)
- Stormcraft (for storm markers)
- Bazaar (for shop markers)
- SilkRoad (for transporter/trade post markers)
- StormcraftEvents (for event markers)
- StormcraftDungeons (for dungeon markers)

## Technical Considerations

### Performance
- **Async Rendering**: Render map frames asynchronously to avoid main thread lag
- **Caching**: Cache terrain data to reduce re-rendering
- **Update Throttling**: Limit marker update frequency per plugin
- **Distance Culling**: Only render markers within map bounds
- **Marker Limits**: Cap maximum markers per map to prevent lag

### Map Rendering Details
- Use Bukkit's MapCanvas for pixel manipulation
- Map resolution: 128x128 pixels
- Each pixel can display one of 256 colors
- Custom markers drawn as pixel art overlays
- Player position uses cursor system or pixel overlay

### Data Storage
- Player preferences stored in YAML (lightweight)
- No database required (all data is ephemeral or config-based)
- Marker data pulled from integrated plugins in real-time

### Update Strategy
- **Terrain**: Update only when player moves significantly
- **Player Position**: Update every tick for smooth movement
- **Markers**: Update based on integration plugin events
- **Storms**: High frequency updates (every 20 ticks) for movement tracking

## Future Enhancements

### Possible Future Features
1. **Waypoint System**: Players can set custom waypoints
2. **Shared Maps**: Party/guild shared map views
3. **Map History**: Record and replay storm paths
4. **3D Terrain**: Height-based shading for terrain
5. **Night/Day Cycle**: Map colors change based on time
6. **Biome Coloring**: Enhanced biome visualization
7. **Mob Tracking**: Optional hostile mob markers
8. **Death Markers**: Show player death locations
9. **Claims Integration**: Show WorldGuard/Land claims
10. **Web Map Export**: Export map data to web viewer

## Development Notes

### Icon Design
All icons should be 8x8 or 16x16 pixel art:
- **Storm**: Swirling cloud with lightning bolt
- **Shop**: Small building/chest icon
- **Transporter**: Portal/telepad icon
- **Trade Post**: Trading stand/booth icon
- **Event**: Star/exclamation mark
- **Dungeon**: Skull/crossed swords icon
- **Player**: Arrow or circle

### Color Palette
Use Minecraft map colors for consistency:
- RED: Storms (danger)
- GREEN: Shops (economy)
- BLUE: Transporters (travel)
- YELLOW: Trade Posts (commerce)
- PURPLE: Events (special)
- DARK_RED: Dungeons (challenge)
- WHITE: Player position

## Integration Testing Plan

### Test Cases
1. **Stormcraft**: Create test storms, verify marker position/movement
2. **Bazaar**: Create/delete shops, verify markers appear/disappear
3. **SilkRoad**: Test transporter/trade post markers
4. **StormcraftEvents**: Start/stop events, verify markers
5. **StormcraftDungeons**: Test dungeon markers and status updates
6. **Performance**: Test with 50+ players and 100+ markers
7. **Cross-Plugin**: Test all integrations simultaneously

## Success Criteria

The plugin will be considered successful when:
1. ✅ All players receive a map on join
2. ✅ Map shows player position in real-time
3. ✅ All 5 plugin integrations working
4. ✅ Storms visible with direction indicators
5. ✅ No noticeable server lag from map updates
6. ✅ Players can toggle marker types
7. ✅ Configuration fully customizable
8. ✅ Map updates smoothly as player moves

## Timeline Estimate

- **Phase 1**: 2-3 hours (Core map system)
- **Phase 2**: 2-3 hours (Marker system)
- **Phase 3**: 1-2 hours (Stormcraft integration)
- **Phase 4**: 2-3 hours (Shop & economy integrations)
- **Phase 5**: 2-3 hours (Event & dungeon integrations)
- **Phase 6**: 1-2 hours (Polish & optimization)

**Total**: 10-16 hours of development time

## Support & Maintenance

### Versioning
- Follow semantic versioning (MAJOR.MINOR.PATCH)
- Maintain compatibility with Paper 1.21.3+
- Update when integrated plugins have breaking API changes

### Documentation
- Comprehensive README.md with setup instructions
- JavaDoc for all public API methods
- Configuration comments for all settings
- Integration guide for plugin developers
