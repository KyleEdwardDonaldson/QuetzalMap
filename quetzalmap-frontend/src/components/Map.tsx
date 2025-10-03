import { MapContainer, TileLayer, useMapEvents } from 'react-leaflet';
import { useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import ScaleBar from './ScaleBar';

interface MapProps {
  apiUrl?: string;
  world?: string;
  zoom?: number;
  center?: [number, number];
  onScaleUpdate?: (width: number, text: string) => void;
}

/**
 * Custom CRS for 512px tiles with proper zoom scaling
 * Extends CRS.Simple to handle 512x512 tiles correctly
 */
const QuetzalCRS = L.extend({}, L.CRS.Simple, {
  transformation: new L.Transformation(1, 0, 1, 0),

  scale: function(zoom: number) {
    // Scale function for 512px tiles: 512 * 2^zoom
    // This ensures tiles scale correctly at all zoom levels
    return 512 * Math.pow(2, zoom);
  },

  zoom: function(scale: number) {
    // Inverse scale function: converts scale back to zoom level
    return Math.log(scale / 512) / Math.LN2;
  }
});

/**
 * Dynamic zoom tracker component
 * Updates keepBuffer based on current zoom level
 */
function ZoomTracker({ onZoomChange }: { onZoomChange: (zoom: number) => void }) {
  useMapEvents({
    zoomend: (e) => {
      onZoomChange(e.target.getZoom());
    }
  });
  return null;
}

/**
 * QuetzalMap tile layer component
 * Fetches tiles from the backend server
 */
function QuetzalTileLayer({ apiUrl, world }: { apiUrl: string; world: string }) {
  const [currentZoom, setCurrentZoom] = useState(0);

  // Calculate keepBuffer based on zoom level
  // More zoomed out (negative zoom) = need more buffer
  const keepBuffer = currentZoom <= -3 ? 32 :
                     currentZoom <= -2 ? 24 :
                     currentZoom <= -1 ? 16 : 8;

  return (
    <>
      <ZoomTracker onZoomChange={setCurrentZoom} />
      <TileLayer
        url={`${apiUrl}/tiles/${world}/0/{x}_{y}.png`}
        attribution='&copy; QuetzalMap'
        tileSize={512}
        minNativeZoom={0}
        maxNativeZoom={0}
        minZoom={-3}
        maxZoom={3}
        noWrap={true}
        keepBuffer={keepBuffer}
        updateWhenIdle={false}
      />
    </>
  );
}

/**
 * Main map component
 * Displays the Minecraft world map using Leaflet
 */
export default function Map({
  apiUrl = 'http://localhost:8080',
  world = 'world',
  zoom = 0,
  center = [0, 0],
  onScaleUpdate
}: MapProps) {
  return (
    <MapContainer
      center={center}
      zoom={zoom}
      className="w-full h-full"
      crs={QuetzalCRS}
      minZoom={-3}
      maxZoom={3}
    >
      <QuetzalTileLayer apiUrl={apiUrl} world={world} />
      {onScaleUpdate && <ScaleBar onUpdate={onScaleUpdate} />}
    </MapContainer>
  );
}
