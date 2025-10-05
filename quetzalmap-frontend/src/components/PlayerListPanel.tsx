import { useState, useEffect } from 'react';
import type { SSEEvent } from '../hooks/useSSE';

export interface Player {
  uuid: string;
  name: string;
  x: number;
  y: number;
  z: number;
  world: string;
  yaw: number;
}

interface PlayerListPanelProps {
  events: SSEEvent[];
  onPlayerClick?: (player: Player) => void;
  currentWorld: string;
  visible: boolean;
  onClose: () => void;
}

/**
 * Panel displaying online players with click-to-center functionality
 */
export default function PlayerListPanel({ events, onPlayerClick, currentWorld, visible, onClose }: PlayerListPanelProps) {
  const [players, setPlayers] = useState<Map<string, Player>>(new Map());

  // Update player list from SSE events
  useEffect(() => {
    events.forEach(event => {
      if (event.type === 'player_list') {
        // Initial player list
        const playerMap = new Map<string, Player>();
        (event.data.players || []).forEach((p: Player) => {
          playerMap.set(p.uuid, p);
        });
        setPlayers(playerMap);
      } else if (event.type === 'player_join') {
        // Add newly joined player
        setPlayers(prev => {
          const updated = new Map(prev);
          updated.set(event.data.uuid, event.data);
          return updated;
        });
      } else if (event.type === 'player_moved') {
        // Update specific player position
        setPlayers(prev => {
          const updated = new Map(prev);
          updated.set(event.data.uuid, event.data);
          return updated;
        });
      } else if (event.type === 'player_disconnect') {
        // Remove disconnected player
        setPlayers(prev => {
          const updated = new Map(prev);
          updated.delete(event.data.uuid);
          return updated;
        });
      }
    });
  }, [events]);

  // Filter players by current world
  const worldPlayers = Array.from(players.values()).filter(p => p.world === currentWorld);

  if (!visible) return null;

  return (
    <div className="absolute top-4 right-4 z-[1000] w-64 mt-48">
      <div className="bg-white/90 backdrop-blur-sm rounded-lg shadow-lg overflow-hidden">
        {/* Header */}
        <div className="px-4 py-3 flex items-center justify-between border-b border-gray-200">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-green-500"></div>
            <span className="font-semibold text-gray-900">
              Players ({worldPlayers.length})
            </span>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Player list */}
        <div className="max-h-96 overflow-y-auto">
            {worldPlayers.length === 0 ? (
              <div className="px-4 py-3 text-sm text-gray-500">
                No players online
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {worldPlayers
                  .sort((a, b) => a.name.localeCompare(b.name))
                  .map(player => (
                    <button
                      key={player.uuid}
                      onClick={() => onPlayerClick?.(player)}
                      className="w-full px-4 py-2 text-left hover:bg-blue-50 transition-colors group"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2 min-w-0 flex-1">
                          {/* Player head from Crafatar */}
                          <img
                            src={`https://crafatar.com/avatars/${player.uuid}?size=32&overlay`}
                            alt={player.name}
                            className="w-6 h-6 rounded flex-shrink-0"
                            style={{
                              imageRendering: 'pixelated',
                              border: '1px solid rgba(0,0,0,0.1)'
                            }}
                          />
                          {/* Player name */}
                          <span className="text-sm font-medium text-gray-900 truncate">
                            {player.name}
                          </span>
                        </div>
                        {/* Location icon - only visible on hover */}
                        <svg
                          className="w-4 h-4 text-gray-400 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                        >
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                      </div>
                      {/* Coordinates */}
                      <div className="text-xs text-gray-500 mt-1 ml-8">
                        {Math.round(player.x)}, {Math.round(player.y)}, {Math.round(player.z)}
                      </div>
                    </button>
                  ))}
              </div>
            )}
          </div>
      </div>
    </div>
  );
}
