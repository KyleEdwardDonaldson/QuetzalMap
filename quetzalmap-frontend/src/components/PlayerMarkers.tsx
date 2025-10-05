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
 * Create a player head icon using Crafatar API
 */
const createPlayerHeadIcon = (uuid: string) => {
  return new L.Icon({
    // Use Crafatar for reliable Minecraft head rendering
    // size=32 for crisp rendering at 24x24 display size
    iconUrl: `https://crafatar.com/avatars/${uuid}?size=32&overlay`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
    tooltipAnchor: [0, -12],
    className: 'player-head-marker'
  });
};

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
          position={[-player.z, player.x]}
          icon={createPlayerHeadIcon(player.uuid)}
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
