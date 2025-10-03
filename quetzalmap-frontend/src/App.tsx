import { useState } from 'react';
import Map from './components/Map';
import MapControls from './components/MapControls';
import { useSSE } from './hooks/useSSE';

// API URL configuration
// Use direct backend URL for both dev and prod to avoid proxy issues with SSE
const API_URL = import.meta.env.VITE_API_URL || 'http://216.238.79.60:8123';

function App() {
  const [world, setWorld] = useState('world');
  const [scaleWidth, setScaleWidth] = useState(0);
  const [scaleText, setScaleText] = useState('');

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
        scaleWidth={scaleWidth}
        scaleText={scaleText}
      />
    </div>
  );
}

export default App;
