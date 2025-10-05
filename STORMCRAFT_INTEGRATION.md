# Stormcraft Integration Plan for QuetzalMap

## Overview

Integrate Stormcraft's traveling storm system with QuetzalMap to display real-time storm positions, movement, phases, and intensity on the web map.

---

## Storm Data Available from Stormcraft

### TravelingStorm Class
```java
// Location & Movement
- Location getCurrentLocation()      // Storm center coordinates
- Location getTargetLocation()       // Next waypoint
- double getMovementSpeed()          // Blocks per second
- boolean move(double deltaSeconds)  // Movement update

// Size & Damage
- double getDamageRadius()           // Base radius
- double getCurrentRadius()          // Radius with phase multiplier
- double getRadiusMultiplier()       // 0.2-1.0 based on phase
- double getActualDamagePerSecond()  // Base damage
- double getCurrentDamagePerSecond() // Damage with phase multiplier

// Phase System
- StormPhase getCurrentPhase()       // FORMING, PEAK, DISSIPATING
- double getPhaseMultiplier()        // Damage scaling (0.0-1.0)

// Lifecycle
- int getRemainingSeconds()
- boolean isExpired()
- boolean hasReachedTarget()

// Storm Type
- StormProfile getProfile()
  - StormType getType()              // SHORT_WEAK, MEDIUM, LONG_DANGEROUS
```

### Events Available
```java
// Storm lifecycle events
1. StormcraftStormStartEvent         // Storm becomes active
   - ActiveStorm getActiveStorm()

2. StormcraftStormTickEvent          // Every exposure check (1 sec)
   - ActiveStorm getActiveStorm()
   - List<Player> getExposedPlayers()

3. StormcraftStormEndEvent           // Storm expires
   - ActiveStorm getActiveStorm()
```

### StormPhase Enum
```java
FORMING      - "§7⬆" - Building up (20% → 100% radius, 0% → 100% damage)
PEAK         - "§c⚡" - Full intensity (100% radius, 100% damage)
DISSIPATING  - "§7⬇" - Weakening (100% → 50% radius, 100% → 0% damage)
```

### StormType Enum
```java
SHORT_WEAK       - Fast, low damage, small radius
MEDIUM           - Moderate duration/damage/radius
LONG_DANGEROUS   - Slow, high damage, large radius
```

---

## Integration Architecture

### Backend: QuetzalMap Plugin Integration

#### 1. Create `StormcraftIntegration.java`

```java
package dev.ked.quetzalmap.integration;

import dev.ked.stormcraft.api.events.*;
import dev.ked.stormcraft.model.*;
import dev.ked.stormcraft.schedule.TravelingStormManager;
import dev.ked.quetzalmap.server.sse.SSEManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class StormcraftIntegration implements Listener {
    private final SSEManager sseManager;
    private final Plugin plugin;
    private TravelingStormManager stormManager;

    public StormcraftIntegration(SSEManager sseManager, Plugin plugin) {
        this.sseManager = sseManager;
        this.plugin = plugin;
    }

    /**
     * Hook into Stormcraft to get TravelingStormManager
     */
    public void initialize(Plugin stormcraft) {
        // Get storm manager from Stormcraft plugin
        if (stormcraft instanceof dev.ked.stormcraft.StormcraftPlugin) {
            this.stormManager = ((dev.ked.stormcraft.StormcraftPlugin) stormcraft)
                .getTravelingStormManager();
        }
    }

    @EventHandler
    public void onStormStart(StormcraftStormStartEvent event) {
        // Note: This event fires for stationary storms (ActiveStorm)
        // We need traveling storms, so we'll use tick events instead
    }

    @EventHandler
    public void onStormTick(StormcraftStormTickEvent event) {
        // Get traveling storm from manager
        TravelingStorm storm = stormManager.getActiveStorm();
        if (storm == null) return;

        // Build storm data JSON
        String json = buildStormJson(storm);

        // Broadcast to all web map clients
        sseManager.broadcast("storm_update", json);
    }

    @EventHandler
    public void onStormEnd(StormcraftStormEndEvent event) {
        // Notify map that storm ended
        String json = "{\"ended\":true}";
        sseManager.broadcast("storm_end", json);
    }

    /**
     * Build JSON representation of storm for web map
     */
    private String buildStormJson(TravelingStorm storm) {
        Location center = storm.getCurrentLocation();
        Location target = storm.getTargetLocation();
        StormPhase phase = storm.getCurrentPhase();
        StormType type = storm.getProfile().getType();

        return String.format(
            "{" +
            "\"x\":%.2f," +
            "\"z\":%.2f," +
            "\"targetX\":%.2f," +
            "\"targetZ\":%.2f," +
            "\"radius\":%.2f," +
            "\"baseRadius\":%.2f," +
            "\"radiusMultiplier\":%.2f," +
            "\"phase\":\"%s\"," +
            "\"phaseSymbol\":\"%s\"," +
            "\"phaseMultiplier\":%.2f," +
            "\"type\":\"%s\"," +
            "\"damage\":%.2f," +
            "\"speed\":%.2f," +
            "\"remainingSeconds\":%d," +
            "\"world\":\"%s\"" +
            "}",
            center.getX(),
            center.getZ(),
            target.getX(),
            target.getZ(),
            storm.getCurrentRadius(),
            storm.getDamageRadius(),
            storm.getRadiusMultiplier(),
            phase.name(),
            phase.getSymbol(),
            storm.getPhaseMultiplier(),
            type.name(),
            storm.getCurrentDamagePerSecond(),
            storm.getMovementSpeed(),
            storm.getRemainingSeconds(),
            center.getWorld().getName()
        );
    }
}
```

#### 2. Register Integration in `QuetzalMapPlugin.java`

```java
// In initializeComponents()
private StormcraftIntegration stormcraftIntegration;

// Check if Stormcraft is loaded
Plugin stormcraft = Bukkit.getPluginManager().getPlugin("Stormcraft");
if (stormcraft != null && stormcraft.isEnabled()) {
    stormcraftIntegration = new StormcraftIntegration(webServer.getSSEManager(), this);
    stormcraftIntegration.initialize(stormcraft);
    Bukkit.getPluginManager().registerEvents(stormcraftIntegration, this);
    LOGGER.info("Stormcraft integration enabled");
}
```

#### 3. Add Stormcraft Dependency to `pom.xml`

```xml
<dependency>
    <groupId>dev.ked</groupId>
    <artifactId>stormcraft</artifactId>
    <version>0.1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../../Stormcraft/target/stormcraft-0.1.0.jar</systemPath>
</dependency>
```

---

## Frontend: Web Map Visualization

### 1. Create `StormMarkers.tsx`

```typescript
import { useEffect, useState } from 'react';
import { Circle, Marker, Polyline, Popup } from 'react-leaflet';
import L from 'leaflet';
import type { SSEEvent } from '../hooks/useSSE';

interface Storm {
  x: number;
  z: number;
  targetX: number;
  targetZ: number;
  radius: number;
  baseRadius: number;
  radiusMultiplier: number;
  phase: 'FORMING' | 'PEAK' | 'DISSIPATING';
  phaseSymbol: string;
  phaseMultiplier: number;
  type: 'SHORT_WEAK' | 'MEDIUM' | 'LONG_DANGEROUS';
  damage: number;
  speed: number;
  remainingSeconds: number;
  world: string;
}

interface StormMarkersProps {
  events: SSEEvent[];
  world: string;
}

// Storm icon with phase indicator
const createStormIcon = (phase: string, type: string) => {
  const color = phase === 'PEAK' ? '#ff4444' :
                phase === 'FORMING' ? '#ffaa00' : '#888888';

  const size = type === 'LONG_DANGEROUS' ? 32 :
               type === 'MEDIUM' ? 28 : 24;

  return new L.DivIcon({
    className: 'storm-marker',
    html: `
      <div style="
        width: ${size}px;
        height: ${size}px;
        background: radial-gradient(circle, ${color}dd, ${color}44);
        border: 2px solid ${color};
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: ${size * 0.6}px;
        box-shadow: 0 0 20px ${color}88;
        animation: pulse-storm 2s infinite;
      ">
        ⚡
      </div>
    `,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2]
  });
};

export default function StormMarkers({ events, world }: StormMarkersProps) {
  const [storm, setStorm] = useState<Storm | null>(null);

  useEffect(() => {
    events.forEach(event => {
      if (event.type === 'storm_update') {
        setStorm(event.data);
      } else if (event.type === 'storm_end') {
        setStorm(null);
      }
    });
  }, [events]);

  if (!storm || storm.world !== world) return null;

  // Storm center position (Leaflet uses [-z, x])
  const centerPos: [number, number] = [-storm.z, storm.x];
  const targetPos: [number, number] = [-storm.targetZ, storm.targetX];

  // Phase-based styling
  const phaseColor = storm.phase === 'PEAK' ? '#ff4444' :
                     storm.phase === 'FORMING' ? '#ffaa00' : '#888888';

  const fillOpacity = storm.phaseMultiplier * 0.3; // Fades in/out with phase

  return (
    <>
      {/* Storm radius circle */}
      <Circle
        center={centerPos}
        radius={storm.radius}
        pathOptions={{
          color: phaseColor,
          fillColor: phaseColor,
          fillOpacity: fillOpacity,
          weight: 2,
          opacity: 0.8
        }}
      />

      {/* Direction arrow to target */}
      <Polyline
        positions={[centerPos, targetPos]}
        pathOptions={{
          color: phaseColor,
          weight: 3,
          opacity: 0.6,
          dashArray: '10, 10'
        }}
      />

      {/* Storm center marker */}
      <Marker
        position={centerPos}
        icon={createStormIcon(storm.phase, storm.type)}
      >
        <Popup>
          <div className="storm-popup">
            <h3 className="font-bold mb-2">
              {storm.phaseSymbol} Storm {storm.phase}
            </h3>
            <div className="text-sm space-y-1">
              <div>Type: <strong>{storm.type.replace('_', ' ')}</strong></div>
              <div>Radius: <strong>{Math.round(storm.radius)}m</strong></div>
              <div>Damage: <strong>{storm.damage.toFixed(1)}/s</strong></div>
              <div>Speed: <strong>{storm.speed.toFixed(1)} m/s</strong></div>
              <div>Time Left: <strong>{formatTime(storm.remainingSeconds)}</strong></div>
              <div className="mt-2 pt-2 border-t">
                <div className="text-xs text-gray-600">
                  Phase: {(storm.phaseMultiplier * 100).toFixed(0)}% intensity
                </div>
                <div className="text-xs text-gray-600">
                  Size: {(storm.radiusMultiplier * 100).toFixed(0)}% of max
                </div>
              </div>
            </div>
          </div>
        </Popup>
      </Marker>
    </>
  );
}

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}
```

### 2. Add CSS Animations (index.css)

```css
/* Storm marker pulse animation */
@keyframes pulse-storm {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.storm-marker {
  animation: pulse-storm 2s infinite;
}

/* Storm popup styling */
.storm-popup {
  min-width: 200px;
}

.storm-popup h3 {
  color: #ff4444;
  margin: 0;
}
```

### 3. Update `App.tsx` to Include Storm Markers

```typescript
import StormMarkers from './components/StormMarkers';

// In the Map component
<Map center={center} zoom={zoom} ref={mapRef}>
  {/* Existing tile layer */}

  <PlayerMarkers events={events} world={currentWorld} />
  <StormMarkers events={events} world={currentWorld} />
</Map>
```

---

## SSE Event Format

### `storm_update`
Broadcast every 1 second (on StormTickEvent):

```json
{
  "x": 2650.5,
  "z": -2155.3,
  "targetX": 0.0,
  "targetZ": 0.0,
  "radius": 450.5,
  "baseRadius": 500.0,
  "radiusMultiplier": 0.9,
  "phase": "PEAK",
  "phaseSymbol": "§c⚡",
  "phaseMultiplier": 1.0,
  "type": "LONG_DANGEROUS",
  "damage": 5.0,
  "speed": 2.5,
  "remainingSeconds": 450,
  "world": "world"
}
```

### `storm_end`
Broadcast when storm expires:

```json
{
  "ended": true
}
```

---

## Visual Design Specifications

### Storm Circle Colors
- **FORMING** (`phaseMultiplier 0.0 → 1.0`):
  - Color: `#ffaa00` (orange)
  - Opacity: Fades in from 0% to 30%
  - Radius: Grows from 20% to 100%

- **PEAK** (`phaseMultiplier 1.0`):
  - Color: `#ff4444` (red)
  - Opacity: 30%
  - Radius: 100%

- **DISSIPATING** (`phaseMultiplier 1.0 → 0.0`):
  - Color: `#888888` (gray)
  - Opacity: Fades out from 30% to 5%
  - Radius: Shrinks from 100% to 50%

### Storm Center Icon
- **Size based on type**:
  - SHORT_WEAK: 24px
  - MEDIUM: 28px
  - LONG_DANGEROUS: 32px

- **Animation**: Pulsing glow effect
- **Symbol**: Lightning bolt ⚡
- **Glow**: Phase-colored shadow

### Direction Indicator
- **Dashed line** from storm center to target waypoint
- **Color**: Matches phase color
- **Opacity**: 60%
- **Pattern**: 10px dash, 10px gap

---

## Performance Optimizations

### Backend
1. **Throttle broadcasts**: Only send updates every 1 second (matches StormTickEvent)
2. **Single event listener**: One handler for all storms
3. **JSON caching**: Cache formatted JSON for 1 second to avoid rebuilding

### Frontend
1. **Single storm state**: Only one active storm at a time
2. **Memoized icons**: Create icons once per phase/type combination
3. **Conditional rendering**: Only render if storm.world === currentWorld

---

## Testing Plan

### Backend Tests
1. ✅ Storm start → `storm_update` broadcast
2. ✅ Storm movement → Position updates
3. ✅ Storm phase transitions → Correct multipliers
4. ✅ Storm end → `storm_end` broadcast
5. ✅ Multiple clients → All receive updates

### Frontend Tests
1. ✅ Storm appears on map at correct location
2. ✅ Circle radius scales with phase
3. ✅ Direction arrow points to target
4. ✅ Popup shows correct stats
5. ✅ Storm disappears on end event
6. ✅ Phase colors transition correctly

### Integration Tests
1. ✅ Storm crosses world boundary → Disappears/reappears
2. ✅ Player clicks storm → Popup opens
3. ✅ Storm reaches target → Direction arrow updates
4. ✅ Multiple storms → Only current one shows
5. ✅ Plugin reload → Storm state persists

---

## Configuration Options

### QuetzalMap config.yml
```yaml
integrations:
  stormcraft:
    enabled: true
    update-rate: 1  # seconds (matches StormTickEvent)
    show-radius: true
    show-direction: true
    show-popup: true
    marker-type: "circle"  # circle, icon, both
```

---

## Future Enhancements

### Short-term
- Multiple storm support (if Stormcraft adds it)
- Storm intensity heatmap (damage gradient)
- Historical storm paths (trail effect)

### Medium-term
- Storm prediction paths (show all waypoints)
- Sound alerts when storm approaches player
- Mobile notifications for storms

### Long-term
- 3D storm visualization (height-based effects)
- Storm collision detection with towns
- Storm statistics dashboard

---

## Implementation Checklist

### Backend (QuetzalMap Paper Plugin)
- [ ] Create `StormcraftIntegration.java`
- [ ] Add Stormcraft dependency to `pom.xml`
- [ ] Register integration in `QuetzalMapPlugin.java`
- [ ] Add soft dependency in `plugin.yml`
- [ ] Test event listeners
- [ ] Test JSON formatting
- [ ] Test SSE broadcasts

### Frontend (React Web Map)
- [ ] Create `StormMarkers.tsx` component
- [ ] Add storm CSS animations
- [ ] Import into `App.tsx`
- [ ] Test storm rendering
- [ ] Test phase transitions
- [ ] Test popup interactions

### Documentation
- [ ] Update README.md with storm features
- [ ] Update CLAUDE.md with integration details
- [ ] Add storm visualization screenshots
- [ ] Document SSE event formats

### Testing
- [ ] Unit tests for StormcraftIntegration
- [ ] Integration tests with live Stormcraft
- [ ] Frontend visual tests
- [ ] Performance benchmarks
- [ ] Multi-user testing

---

**Estimated Implementation Time**: 3-4 hours
- Backend: 1.5 hours
- Frontend: 1.5 hours
- Testing/Polish: 1 hour
