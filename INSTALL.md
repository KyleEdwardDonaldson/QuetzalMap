# QuetzalMap Installation Guide

## Quick Start

### 1. Prerequisites
- Paper 1.21.3+ server
- Java 21+
- **Optional**: WorldGuard 7.0.9+ for region markers
- **Optional**: Towny 0.100+ for town/nation markers

### 2. Installation Steps

1. **Download the plugin**
   - Get `QuetzalMap-1.0.0.jar` from the releases

2. **Install the plugin**
   ```bash
   # Copy jar to plugins directory
   cp QuetzalMap-1.0.0.jar /path/to/server/plugins/
   ```

3. **Restart your server**
   ```bash
   # Stop server
   ./stop.sh

   # Start server
   ./start.sh
   ```

4. **Verify installation**
   - Check console for: `QuetzalMap has been enabled!`
   - Check for WorldGuard/Towny integration messages
   - Run `/qmap` in-game to test

### 3. First-Time Configuration

The default configuration is created at `plugins/QuetzalMap/config.yml`. Key settings to review:

```yaml
map:
  auto-give-on-join: true  # Give maps to new players?
  default-zoom: 1024       # Starting zoom level

markers:
  regions:
    enabled: true          # Show WorldGuard regions?
    max-regions: 50       # Maximum regions to display

  towns:
    enabled: true          # Show Towny towns?
    max-towns: 100        # Maximum towns to display

  nations:
    enabled: true          # Show Towny nations?
    max-nations: 50       # Maximum nations to display
```

### 4. Give Players Maps

Players automatically receive maps on join if `auto-give-on-join: true`.

Manually give maps:
```
/qmap give <player>
```

### 5. Configure Permissions

Default permissions allow all players to use maps. To restrict:

```yaml
# permissions.yml
groups:
  default:
    permissions:
      - quetzalmap.use
      - quetzalmap.toggle
      - quetzalmap.zoom
      - quetzalmap.markers.*

  admin:
    permissions:
      - quetzalmap.admin
```

## Integration Setup

### WorldGuard Regions

1. Install WorldGuard 7.0.9+
2. Create some regions:
   ```
   /rg define myregion
   ```
3. Restart server or reload QuetzalMap:
   ```
   /qmap reload
   ```
4. Regions will appear on maps with red X markers

**Configure region display:**
```yaml
integrations:
  worldguard:
    enabled: true
    update-interval: 100  # Update every 5 seconds

markers:
  regions:
    enabled: true
    only-show-owned: false  # Show all regions
    display-types:
      - polygon
      - cuboid
    max-regions: 50
```

### Towny Towns & Nations

1. Install Towny 0.100+
2. Create towns and nations:
   ```
   /town new MyTown
   /nation new MyNation
   ```
3. Restart server or reload QuetzalMap:
   ```
   /qmap reload
   ```
4. Towns appear with green banners, nations with blue banners

**Configure town/nation display:**
```yaml
integrations:
  towny:
    enabled: true
    update-interval: 100

markers:
  towns:
    enabled: true
    show-spawn: true      # Use town spawn as marker location
    only-show-own: false  # Show all towns
    max-towns: 100

  nations:
    enabled: true
    show-capital: true    # Show nation capitals
    only-show-own: false  # Show all nations
    max-nations: 50
```

## Troubleshooting

### "Plugin not loading"
- Check Java version: `java -version` (must be 21+)
- Check Paper version: Should be 1.21.3+
- Check console for errors

### "No markers showing"
- Verify integration plugin is installed and enabled
- Check config: `markers.<type>.enabled: true`
- Ensure player has permission: `quetzalmap.markers.<type>`
- Check marker limits in config

### "Map not updating"
- Verify player has map item in inventory
- Check `map.update-rate` isn't too high
- Try `/qmap give <player>` to get fresh map

### "Server lag"
- Reduce `max-markers-per-map`
- Increase `update-rate`
- Enable `async-rendering: true`
- Reduce integration `update-interval`

## Testing the Installation

1. **Join the server**
   - You should receive a map item automatically

2. **Open the map**
   - Hold the map item
   - You should see your player location

3. **Check markers**
   ```
   /qmap info
   ```
   - Shows enabled markers and counts

4. **Test commands**
   ```
   /qmap toggle regions
   /qmap zoom 2048
   /qmap info
   ```

## Performance Tuning

### For Large Servers (50+ players)
```yaml
performance:
  async-rendering: true
  cache-terrain: true
  max-markers-per-map: 300
  max-marker-distance: 3000

map:
  update-rate: 5  # Update every 5 ticks instead of every tick
```

### For Small Servers (< 20 players)
```yaml
performance:
  async-rendering: true
  cache-terrain: true
  max-markers-per-map: 500
  max-marker-distance: 5000

map:
  update-rate: 1  # Update every tick for smooth experience
```

## Updating the Plugin

1. **Backup config**
   ```bash
   cp plugins/QuetzalMap/config.yml config.yml.backup
   ```

2. **Stop server**
   ```bash
   ./stop.sh
   ```

3. **Replace jar**
   ```bash
   rm plugins/QuetzalMap-*.jar
   cp QuetzalMap-NEW-VERSION.jar plugins/
   ```

4. **Start server**
   ```bash
   ./start.sh
   ```

5. **Check for config changes**
   - Review console for config warnings
   - Compare with backup if needed

## Uninstallation

1. **Stop server**
2. **Remove plugin files**
   ```bash
   rm plugins/QuetzalMap-*.jar
   rm -rf plugins/QuetzalMap/
   ```
3. **Start server**

## Support

If you encounter issues:
1. Check the logs in `logs/latest.log`
2. Review this guide and README.md
3. Check configuration in `plugins/QuetzalMap/config.yml`
4. Report issues on GitHub with:
   - Server version
   - Plugin version
   - Error messages
   - Steps to reproduce
