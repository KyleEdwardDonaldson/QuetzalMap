import { useState, useEffect, useRef } from 'react';
import Map from './components/Map';
import MapControls from './components/MapControls';
import PlayerListPanel, { type Player } from './components/PlayerListPanel';
import { useSSE, type SSEEvent } from './hooks/useSSE';
import type { Map as LeafletMap } from 'leaflet';

// API URL configuration
// Use direct backend URL for both dev and prod to avoid proxy issues with SSE
const API_URL = import.meta.env.VITE_API_URL || 'http://216.238.79.60:8123';

function App() {
  const [world, setWorld] = useState('world');
  const [availableWorlds, setAvailableWorlds] = useState<string[]>(['world']);
  const [scaleWidth, setScaleWidth] = useState(0);
  const [scaleText, setScaleText] = useState('');
  const [showPlayerPanel, setShowPlayerPanel] = useState(false);
  const [playerCount, setPlayerCount] = useState(0);
  const mapRef = useRef<LeafletMap | null>(null);

  // Check which worlds have tiles on mount
  useEffect(() => {
    const checkWorlds = async () => {
      try {
        const response = await fetch(`${API_URL}/api/worlds`);
        if (response.ok) {
          const data = await response.json();
          const available = data.worlds || ['world'];

          setAvailableWorlds(available);

          // Set initial world to first available if current not in list
          if (!available.includes(world)) {
            setWorld(available[0]);
          }

          console.log('Available worlds:', available);
        }
      } catch (err) {
        console.error('Failed to fetch worlds:', err);
        // Fallback to just Overworld
        setAvailableWorlds(['world']);
      }
    };

    checkWorlds();
  }, []);

  // Connect to SSE for live updates
  const { connected, events } = useSSE(`${API_URL}/events`, true);

  // Update player count from events
  useEffect(() => {
    events.forEach((event: SSEEvent) => {
      if (event.type === 'player_list') {
        setPlayerCount((event.data.players || []).length);
      } else if (event.type === 'player_join') {
        setPlayerCount(prev => prev + 1);
      } else if (event.type === 'player_disconnect') {
        setPlayerCount(prev => Math.max(0, prev - 1));
      }
    });
  }, [events]);

  // Handle player click - center map on player
  const handlePlayerClick = (player: Player) => {
    if (mapRef.current) {
      // Minecraft coordinates to map coordinates
      // Leaflet uses [lat, lng] which maps to [-Z, X] in our CRS
      mapRef.current.setView([-player.z, player.x], mapRef.current.getZoom(), {
        animate: true,
        duration: 0.5
      });
    }
  };

  const handleScaleUpdate = (width: number, text: string) => {
    setScaleWidth(width);
    setScaleText(text);
  };

  return (
    <div className="relative w-full h-full">
      <Map
        apiUrl={API_URL}
        world={world}
        zoom={0}
        center={[0, 0]}
        events={events}
        onScaleUpdate={handleScaleUpdate}
        mapRef={mapRef}
      />
      <MapControls
        connected={connected}
        world={world}
        onWorldChange={setWorld}
        availableWorlds={availableWorlds}
        scaleWidth={scaleWidth}
        scaleText={scaleText}
        playerCount={playerCount}
        onViewPlayers={() => setShowPlayerPanel(true)}
      />
      <PlayerListPanel
        events={events}
        currentWorld={world}
        visible={showPlayerPanel}
        onClose={() => setShowPlayerPanel(false)}
        onPlayerClick={handlePlayerClick}
      />
    </div>
  );
}

export default App;
