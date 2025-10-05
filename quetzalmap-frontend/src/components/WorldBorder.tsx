import { useEffect, useState } from 'react';
import { Rectangle, useMap } from 'react-leaflet';
import type { LatLngBoundsExpression } from 'leaflet';
import L from 'leaflet';

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
 * Also restricts map panning to within the border bounds
 */
export default function WorldBorder({ apiUrl, currentWorld }: WorldBorderProps) {
  const [borders, setBorders] = useState<Map<string, WorldBorderData>>(new Map());
  const map = useMap();

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

  // Set map bounds when border data changes
  useEffect(() => {
    if (!border) {
      // Remove bounds restriction if no border for this world
      map.setMaxBounds(undefined);
      return;
    }

    // Calculate border bounds
    const halfSize = border.size / 2;
    const minX = border.centerX - halfSize;
    const maxX = border.centerX + halfSize;
    const minZ = border.centerZ - halfSize;
    const maxZ = border.centerZ + halfSize;

    // Create Leaflet LatLngBounds object
    const bounds = L.latLngBounds(
      [-maxZ, minX],  // Southwest corner
      [-minZ, maxX]   // Northeast corner
    );

    // Restrict map panning to world border
    map.setMaxBounds(bounds);
    map.setMinZoom(-3); // Ensure user can see full border

  }, [border, map]);

  if (!border) return null;

  // Calculate border bounds for rectangle display
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
