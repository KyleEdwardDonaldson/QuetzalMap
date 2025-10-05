import { MapContainer, TileLayer, useMapEvents, useMap } from 'react-leaflet';
import { useState, useEffect } from 'react';
import L from 'leaflet';
import type { Map as LeafletMap } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import ScaleBar from './ScaleBar';
import { PlayerMarkers } from './PlayerMarkers';
import StormMarkers from './StormMarkers';
import WorldBorder from './WorldBorder';
import type { SSEEvent } from '../hooks/useSSE';

interface MapProps {
  apiUrl?: string;
  world?: string;
  zoom?: number;
  center?: [number, number];
  events?: SSEEvent[];
  onScaleUpdate?: (width: number, text: string) => void;
  mapRef?: React.MutableRefObject<LeafletMap | null>;
}

/**
 * Custom CRS for 512px tiles with proper zoom scaling
 * Extends CRS.Simple to handle 512x512 tiles correctly
 *
 * Coordinate system:
 * - Each tile = 512×512 pixels = 512×512 blocks (one region file)
 * - Minecraft coordinates (blocks) map directly to pixels at zoom 0
 * - Transformation converts Minecraft (lng=X, lat=Z) to pixel space
 */
const QuetzalCRS = L.extend({}, L.CRS.Simple, {
  // Transformation converts Minecraft coordinates to pixel coordinates
  // Scale by 1 (1 block = 1 pixel at zoom 0), no offset
  transformation: new L.Transformation(1, 0, -1, 0),

  scale: function(zoom: number) {
    // Scale function: pixels per block at given zoom level
    // At zoom 0: 1 block = 1 pixel
    // At zoom 1: 1 block = 2 pixels (zoomed in)
    // At zoom -1: 1 block = 0.5 pixels (zoomed out)
    return Math.pow(2, zoom);
  },

  zoom: function(scale: number) {
    // Inverse scale function: converts scale back to zoom level
    return Math.log(scale) / Math.LN2;
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
 * Mouse coordinates display component
 * Shows Minecraft coordinates at cursor position
 */
function MouseCoordinates() {
  const [coords, setCoords] = useState<{ x: number; z: number } | null>(null);

  useMapEvents({
    mousemove: (e) => {
      // Leaflet gives us coordinates in the CRS coordinate space
      // lat is negated by CRS transformation, lng is X coordinate
      setCoords({
        x: Math.round(e.latlng.lng),
        z: Math.round(e.latlng.lat)
      });
    },
    mouseout: () => {
      setCoords(null);
    }
  });

  if (!coords) return null;

  return (
    <div className="absolute bottom-4 left-4 z-[1000] bg-black/70 text-white px-3 py-1.5 rounded text-sm font-mono pointer-events-none">
      X: {coords.x}, Z: {coords.z}
    </div>
  );
}

/**
 * Component to expose map instance to parent via ref
 */
function MapRefSetter({ mapRef }: { mapRef: React.MutableRefObject<LeafletMap | null> }) {
  const map = useMap();
  useEffect(() => {
    mapRef.current = map;
  }, [map, mapRef]);
  return null;
}

/**
 * QuetzalMap tile layer component
 * Fetches tiles from the backend server with smart buffering
 */
function QuetzalTileLayer({ apiUrl, world }: { apiUrl: string; world: string }) {
  const [currentZoom, setCurrentZoom] = useState(0);
  const [viewportSize, setViewportSize] = useState({ width: window.innerWidth, height: window.innerHeight });

  // Track viewport size changes
  useEffect(() => {
    const handleResize = () => {
      setViewportSize({ width: window.innerWidth, height: window.innerHeight });
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Calculate dynamic keepBuffer based on viewport and zoom
  // Prevents wasted bandwidth on small screens, ensures smooth panning on large screens
  const calculateKeepBuffer = () => {
    const tilesX = Math.ceil(viewportSize.width / 512);
    const tilesY = Math.ceil(viewportSize.height / 512);
    const viewportTiles = tilesX * tilesY;

    // Base buffer: 1.5x viewport size
    let buffer = Math.ceil(viewportTiles * 1.5);

    // Increase buffer when zoomed out (more tiles visible at once)
    if (currentZoom <= -3) {
      buffer = Math.ceil(buffer * 2.5); // 2.5x multiplier
    } else if (currentZoom <= -2) {
      buffer = Math.ceil(buffer * 2.0); // 2x multiplier
    } else if (currentZoom <= -1) {
      buffer = Math.ceil(buffer * 1.5); // 1.5x multiplier
    }

    // Cap at reasonable maximum to prevent excessive preloading
    return Math.min(buffer, 48);
  };

  const keepBuffer = calculateKeepBuffer();

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
  events = [],
  onScaleUpdate,
  mapRef
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
      {mapRef && <MapRefSetter mapRef={mapRef} />}
      <QuetzalTileLayer apiUrl={apiUrl} world={world} />
      <WorldBorder apiUrl={apiUrl} currentWorld={world} />
      {onScaleUpdate && <ScaleBar onUpdate={onScaleUpdate} />}
      <PlayerMarkers events={events} world={world} />
      <StormMarkers events={events} world={world} />
      <MouseCoordinates />
    </MapContainer>
  );
}
