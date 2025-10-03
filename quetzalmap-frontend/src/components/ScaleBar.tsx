import { useEffect, useState } from 'react';
import { useMap } from 'react-leaflet';

/**
 * Hook to calculate map scale based on zoom level.
 * Returns scale width in pixels and formatted distance text.
 */
export function useMapScale() {
  const map = useMap();
  const [scaleWidth, setScaleWidth] = useState(0);
  const [scaleText, setScaleText] = useState('');

  useEffect(() => {
    if (!map) return;

    const updateScale = () => {
      try {
        // Get the current zoom level
        const zoom = map.getZoom();

        // With our custom CRS: scale = 512 * 2^zoom
        // This means at zoom 0: 1 map unit = 512 pixels
        // At zoom 1: 1 map unit = 1024 pixels
        // Each tile is 1 map unit = 512 blocks (since tiles are 512x512 blocks)

        // Calculate how many map units fit in the visible width
        const mapScale = 512 * Math.pow(2, zoom);
        const mapUnitsPerPixel = 1 / mapScale;

        // Each map unit represents 512 blocks (tile size in blocks)
        const blocksPerMapUnit = 512;
        const blocksPerPixel = mapUnitsPerPixel * blocksPerMapUnit;

        // Calculate a nice round number for the scale
        // Target: scale bar should span most of the container (220px for 256px container)
        const targetPixels = 220;
        const targetBlocks = blocksPerPixel * targetPixels;

        // Round to nice numbers (1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, etc.)
        const roundedBlocks = getRoundNumber(targetBlocks);
        const pixelWidth = roundedBlocks / blocksPerPixel;

        setScaleWidth(Math.round(pixelWidth));
        setScaleText(formatDistance(roundedBlocks));
      } catch (err) {
        console.error('Error updating scale:', err);
      }
    };

    // Wait for map to be ready
    const handleLoad = () => {
      updateScale();
    };

    // Update on zoom, move, or load
    map.on('zoom', updateScale);
    map.on('move', updateScale);
    map.on('zoomend', updateScale);
    map.on('moveend', updateScale);
    map.whenReady(handleLoad);

    return () => {
      map.off('zoom', updateScale);
      map.off('move', updateScale);
      map.off('zoomend', updateScale);
      map.off('moveend', updateScale);
    };
  }, [map]);

  return { scaleWidth, scaleText };
}

/**
 * Scale bar component (invisible, just provides data)
 */
export default function ScaleBar({ onUpdate }: { onUpdate: (width: number, text: string) => void }) {
  const { scaleWidth, scaleText } = useMapScale();

  useEffect(() => {
    if (scaleWidth > 0 && scaleText) {
      onUpdate(scaleWidth, scaleText);
    }
  }, [scaleWidth, scaleText, onUpdate]);

  return null;
}

/**
 * Round a number to a nice value (1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, etc.)
 */
function getRoundNumber(num: number): number {
  const pow10 = Math.pow(10, Math.floor(Math.log10(num)));
  const normalized = num / pow10;

  if (normalized >= 5) return 5 * pow10;
  if (normalized >= 2) return 2 * pow10;
  return pow10;
}

/**
 * Format distance with appropriate units
 */
function formatDistance(blocks: number): string {
  if (blocks >= 1000) {
    return `${(blocks / 1000).toFixed(blocks >= 10000 ? 0 : 1)}k blocks`;
  }
  return `${Math.round(blocks)} blocks`;
}
