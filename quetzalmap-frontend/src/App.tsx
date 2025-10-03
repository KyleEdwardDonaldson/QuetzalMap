import { useState, useEffect } from 'react';
import Map from './components/Map';
import MapControls from './components/MapControls';
import { useSSE } from './hooks/useSSE';

// API URL configuration
// Use direct backend URL for both dev and prod to avoid proxy issues with SSE
const API_URL = import.meta.env.VITE_API_URL || 'http://216.238.79.60:8123';

// All possible worlds to check
const POSSIBLE_WORLDS = ['world', 'world_nether', 'world_the_end'];

function App() {
  const [world, setWorld] = useState('world');
  const [availableWorlds, setAvailableWorlds] = useState<string[]>(['world']);
  const [scaleWidth, setScaleWidth] = useState(0);
  const [scaleText, setScaleText] = useState('');

  // Check which worlds have tiles on mount
  useEffect(() => {
    const checkWorlds = async () => {
      const available: string[] = [];

      for (const worldName of POSSIBLE_WORLDS) {
        try {
          // Try to fetch a tile from zoom 0 at origin (0_0.png)
          const response = await fetch(`${API_URL}/tiles/${worldName}/0/0_0.png`, {
            method: 'HEAD' // Use HEAD to avoid downloading the image
          });

          if (response.ok) {
            available.push(worldName);
          }
        } catch (err) {
          // World doesn't exist or has no tiles
          console.log(`World ${worldName} not available:`, err);
        }
      }

      if (available.length > 0) {
        setAvailableWorlds(available);
        // Set initial world to first available
        if (!available.includes(world)) {
          setWorld(available[0]);
        }
      }
    };

    checkWorlds();
  }, []);

  // Connect to SSE for live updates
  const { connected, events } = useSSE(`${API_URL}/events`, true);

  // Log events for debugging
  if (events.length > 0) {
    console.log('Recent events:', events.slice(-5));
  }

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
        onScaleUpdate={handleScaleUpdate}
      />
      <MapControls
        connected={connected}
        world={world}
        onWorldChange={setWorld}
        availableWorlds={availableWorlds}
        scaleWidth={scaleWidth}
        scaleText={scaleText}
      />
    </div>
  );
}

export default App;
