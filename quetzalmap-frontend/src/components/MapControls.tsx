interface MapControlsProps {
  connected: boolean;
  world: string;
  onWorldChange: (world: string) => void;
  scaleText?: string;
  scaleWidth?: number;
}

/**
 * Map control panel - displays connection status and controls
 */
export default function MapControls({ connected, world, onWorldChange, scaleText, scaleWidth }: MapControlsProps) {
  return (
    <div className="absolute top-4 right-4 z-[1000] space-y-2">
      {/* Main controls */}
      <div className="bg-white/90 backdrop-blur-sm rounded-lg shadow-lg p-4 min-w-64">
        <div className="space-y-4">
          {/* Header */}
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-gray-900">QuetzalMap</h2>
            <div className="flex items-center gap-2">
              <div className={`w-2 h-2 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`}></div>
              <span className="text-xs text-gray-600">
                {connected ? 'Live' : 'Offline'}
              </span>
            </div>
          </div>

          {/* World selector */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              World
            </label>
            <select
              value={world}
              onChange={(e) => onWorldChange(e.target.value)}
              className="w-full px-3 py-2 bg-white border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="world">Overworld</option>
              <option value="world_nether">Nether</option>
              <option value="world_the_end">The End</option>
            </select>
          </div>

          {/* Info */}
          <div className="text-xs text-gray-500 pt-2 border-t">
            <p>Use scroll to zoom</p>
            <p>Drag to pan</p>
          </div>
        </div>
      </div>

      {/* Scale bar - separate component with more transparency */}
      {scaleText && scaleWidth && (
        <div className="bg-white/70 backdrop-blur-sm rounded-lg shadow-lg p-3 min-w-64">
          <div className="flex flex-col items-center">
            <div className="flex items-end h-6 mb-1">
              <div
                className="border-b-2 border-l-2 border-r-2 border-gray-700 h-3 relative"
                style={{ width: `${scaleWidth}px` }}
              >
                <div className="border-l-2 border-gray-700 h-1 absolute left-1/2 transform -translate-x-1/2 top-0"></div>
              </div>
            </div>
            <div className="text-xs text-gray-700 font-medium">
              {scaleText}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
