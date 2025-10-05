import { useEffect, useState } from 'react';
import { Circle, Marker, Polyline, Popup } from 'react-leaflet';
import L from 'leaflet';
import type { SSEEvent } from '../hooks/useSSE';

interface Storm {
  id: string;
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
    className: 'storm-marker-icon',
    html: `
      <div class="storm-icon-container" style="
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
      ">
        ⚡
      </div>
    `,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2]
  });
};

export default function StormMarkers({ events, world }: StormMarkersProps) {
  const [storms, setStorms] = useState<Map<string, Storm>>(new Map());

  useEffect(() => {
    events.forEach(event => {
      if (event.type === 'storm_update') {
        const stormData = event.data.storms || [];
        const stormMap = new Map<string, Storm>();

        stormData.forEach((storm: Storm) => {
          stormMap.set(storm.id, storm);
        });

        setStorms(stormMap);
      }
    });
  }, [events]);

  // Filter storms by current world
  const worldStorms = Array.from(storms.values()).filter(s => s.world === world);

  return (
    <>
      {worldStorms.map(storm => {
        // Storm center position (Leaflet uses [-z, x])
        const centerPos: [number, number] = [-storm.z, storm.x];
        const targetPos: [number, number] = [-storm.targetZ, storm.targetX];

        // Phase-based styling
        const phaseColor = storm.phase === 'PEAK' ? '#ff4444' :
                          storm.phase === 'FORMING' ? '#ffaa00' : '#888888';

        const fillOpacity = storm.phaseMultiplier * 0.3; // Fades in/out with phase

        return (
          <div key={storm.id}>
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
                    <span className="mr-1">{storm.phaseSymbol}</span>
                    Storm {storm.phase}
                  </h3>
                  <div className="text-sm space-y-1">
                    <div>Type: <strong>{storm.type.replace(/_/g, ' ')}</strong></div>
                    <div>Radius: <strong>{Math.round(storm.radius)}m</strong></div>
                    <div>Damage: <strong>{storm.damage.toFixed(1)}/s</strong></div>
                    <div>Speed: <strong>{storm.speed.toFixed(1)} m/s</strong></div>
                    <div>Time Left: <strong>{formatTime(storm.remainingSeconds)}</strong></div>
                    <div className="mt-2 pt-2 border-t border-gray-300">
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
          </div>
        );
      })}
    </>
  );
}

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}
