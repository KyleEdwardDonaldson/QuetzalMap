import React, { useEffect, useState } from 'react';
import { Marker, Tooltip } from 'react-leaflet';
import L from 'leaflet';
import type { SSEEvent } from '../hooks/useSSE';

interface Player {
  uuid: string;
  name: string;
  x: number;
  y: number;
  z: number;
  yaw: number;
  world: string;
}

interface PlayerMarkersProps {
  events: SSEEvent[];
  world: string;
}

/**
 * Player marker icon
 */
const playerIcon = new L.Icon({
  iconUrl: 'data:image/svg+xml;base64,' + btoa(`
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
      <circle cx="12" cy="12" r="10" fill="#4A90E2" stroke="#ffffff" stroke-width="2"/>
      <path d="M12 8 L14 14 L12 12 L10 14 Z" fill="#ffffff"/>
    </svg>
  `),
  iconSize: [24, 24],
  iconAnchor: [12, 12],
  tooltipAnchor: [0, -12]
});

/**
 * Displays player markers on the map from SSE events.
 * Updates player positions in real-time as player_moved events arrive.
 */
export const PlayerMarkers: React.FC<PlayerMarkersProps> = ({ events, world }) => {
  const [players, setPlayers] = useState<Map<string, Player>>(new Map());

  useEffect(() => {
    // Process SSE events to update player positions
    events.forEach(event => {
      if (event.type === 'player_list') {
        // Initial player list from server
        const playerList = event.data.players as Player[];
        const newPlayers = new Map<string, Player>();
        playerList.forEach(player => {
          if (player.world === world) {
            newPlayers.set(player.uuid, player);
          }
        });
        setPlayers(newPlayers);
      } else if (event.type === 'player_moved') {
        // Player position update
        const player = event.data as Player;
        if (player.world === world) {
          setPlayers(prev => {
            const updated = new Map(prev);
            updated.set(player.uuid, player);
            return updated;
          });
        } else {
          // Player changed worlds - remove from this world's map
          setPlayers(prev => {
            const updated = new Map(prev);
            updated.delete(player.uuid);
            return updated;
          });
        }
      } else if (event.type === 'player_disconnect') {
        // Player disconnected - remove marker
        const { uuid } = event.data;
        setPlayers(prev => {
          const updated = new Map(prev);
          updated.delete(uuid);
          return updated;
        });
      }
    });
  }, [events, world]);

  return (
    <>
      {Array.from(players.values()).map(player => (
        <Marker
          key={player.uuid}
          position={[player.z, player.x]}
          icon={playerIcon}
        >
          <Tooltip direction="top" permanent={false}>
            <div>
              <strong>{player.name}</strong>
              <br />
              <small>
                X: {Math.round(player.x)}, Y: {Math.round(player.y)}, Z: {Math.round(player.z)}
              </small>
            </div>
          </Tooltip>
        </Marker>
      ))}
    </>
  );
};
