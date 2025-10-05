import { useEffect, useState } from 'react';
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

interface StormListPanelProps {
  events: SSEEvent[];
  currentWorld: string;
  visible: boolean;
  onClose: () => void;
  onStormClick?: (storm: Storm) => void;
}

export default function StormListPanel({
  events,
  currentWorld,
  visible,
  onClose,
  onStormClick
}: StormListPanelProps) {
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

  if (!visible) return null;

  // Filter storms by current world
  const worldStorms = Array.from(storms.values()).filter(s => s.world === currentWorld);

  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const getPhaseColor = (phase: string): string => {
    switch (phase) {
      case 'PEAK': return 'text-red-500';
      case 'FORMING': return 'text-orange-500';
      case 'DISSIPATING': return 'text-gray-500';
      default: return 'text-gray-400';
    }
  };

  const getTypeColor = (type: string): string => {
    switch (type) {
      case 'LONG_DANGEROUS': return 'bg-red-900/50 border-red-700';
      case 'MEDIUM': return 'bg-orange-900/50 border-orange-700';
      case 'SHORT_WEAK': return 'bg-yellow-900/50 border-yellow-700';
      default: return 'bg-gray-900/50 border-gray-700';
    }
  };

  return (
    <div className="fixed top-4 right-4 z-[2000] w-80 bg-gray-900/95 backdrop-blur-sm border border-gray-700 rounded-lg shadow-2xl">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-gray-700">
        <div className="flex items-center gap-2">
          <span className="text-2xl">⚡</span>
          <div>
            <h2 className="text-white font-semibold">Active Storms</h2>
            <p className="text-gray-400 text-sm">
              {worldStorms.length} storm{worldStorms.length !== 1 ? 's' : ''} in {currentWorld}
            </p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-white transition-colors p-1"
          aria-label="Close storm list"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Storm List */}
      <div className="max-h-[600px] overflow-y-auto">
        {worldStorms.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            <p className="text-4xl mb-2">☀️</p>
            <p>No active storms</p>
            <p className="text-sm mt-1">Clear skies in {currentWorld}</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-700">
            {worldStorms.map(storm => (
              <div
                key={storm.id}
                onClick={() => onStormClick?.(storm)}
                className={`p-4 hover:bg-gray-800/50 transition-colors cursor-pointer border-l-4 ${getTypeColor(storm.type)}`}
              >
                {/* Storm Header */}
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <span className="text-2xl">⚡</span>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className={`font-semibold ${getPhaseColor(storm.phase)}`}>
                          {storm.phase}
                        </span>
                        <span className="text-gray-400 text-sm">
                          {storm.type.replace(/_/g, ' ')}
                        </span>
                      </div>
                      <div className="text-xs text-gray-500 mt-0.5">
                        X: {Math.round(storm.x)}, Z: {Math.round(storm.z)}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-white font-mono text-sm">
                      {formatTime(storm.remainingSeconds)}
                    </div>
                    <div className="text-xs text-gray-500">remaining</div>
                  </div>
                </div>

                {/* Storm Stats */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-gray-800/50 rounded p-2">
                    <div className="text-gray-400">Radius</div>
                    <div className="text-white font-semibold">{Math.round(storm.radius)}m</div>
                  </div>
                  <div className="bg-gray-800/50 rounded p-2">
                    <div className="text-gray-400">Damage</div>
                    <div className="text-white font-semibold">{storm.damage.toFixed(1)}/s</div>
                  </div>
                  <div className="bg-gray-800/50 rounded p-2">
                    <div className="text-gray-400">Speed</div>
                    <div className="text-white font-semibold">{storm.speed.toFixed(1)} m/s</div>
                  </div>
                  <div className="bg-gray-800/50 rounded p-2">
                    <div className="text-gray-400">Intensity</div>
                    <div className="text-white font-semibold">
                      {(storm.phaseMultiplier * 100).toFixed(0)}%
                    </div>
                  </div>
                </div>

                {/* Target Location */}
                <div className="mt-2 text-xs text-gray-400">
                  Moving to: X: {Math.round(storm.targetX)}, Z: {Math.round(storm.targetZ)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Footer Info */}
      {worldStorms.length > 0 && (
        <div className="p-3 border-t border-gray-700 bg-gray-800/30">
          <p className="text-xs text-gray-400 text-center">
            Click a storm to center on map
          </p>
        </div>
      )}
    </div>
  );
}
