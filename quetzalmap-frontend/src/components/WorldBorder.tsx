import { useEffect, useState } from 'react';
import { Rectangle } from 'react-leaflet';
import type { LatLngBoundsExpression } from 'leaflet';

interface WorldBorderData {
  world: string;
  centerX: number;
  centerZ: number;
  size: number;
}

interface WorldBorderProps {
  apiUrl: string;
  currentWorld: string;
}

/**
 * Renders the world border as a rectangle on the map
 */
export default function WorldBorder({ apiUrl, currentWorld }: WorldBorderProps) {
  const [borders, setBorders] = useState<Map<string, WorldBorderData>>(new Map());

  useEffect(() => {
    const fetchBorders = async () => {
      try {
        const response = await fetch(`${apiUrl}/api/worldborder`);
        if (response.ok) {
          const data = await response.json();
          const borderMap = new Map<string, WorldBorderData>();

          data.borders.forEach((border: WorldBorderData) => {
            borderMap.set(border.world, border);
          });

          setBorders(borderMap);
        }
      } catch (err) {
        console.error('Failed to fetch world borders:', err);
      }
    };

    fetchBorders();
  }, [apiUrl]);

  const border = borders.get(currentWorld);
  if (!border) return null;

  // Calculate border bounds
  // Minecraft coordinates: X = longitude, Z = latitude (negated for Leaflet)
  const halfSize = border.size / 2;
  const minX = border.centerX - halfSize;
  const maxX = border.centerX + halfSize;
  const minZ = border.centerZ - halfSize;
  const maxZ = border.centerZ + halfSize;

  // Leaflet bounds: [[south, west], [north, east]] = [[maxZ (negated), minX], [minZ (negated), maxX]]
  const bounds: LatLngBoundsExpression = [
    [-maxZ, minX],  // Southwest corner
    [-minZ, maxX]   // Northeast corner
  ];

  return (
    <Rectangle
      bounds={bounds}
      pathOptions={{
        color: '#FF0000',
        weight: 3,
        fill: false,
        opacity: 0.8,
        dashArray: '10, 10'
      }}
    />
  );
}
