# QuetzalMap Frontend

Modern web interface for QuetzalMap built with React, TypeScript, Tailwind CSS, and Leaflet.

## Features

- **Interactive map** - Leaflet-based map with smooth panning and zooming
- **Live tile streaming** - Real-time map tiles from backend server
- **SSE updates** - Server-Sent Events for live marker and player updates
- **Responsive UI** - Tailwind CSS with clean, modern design
- **World switching** - Toggle between Overworld, Nether, and End
- **Connection status** - Live indicator showing backend connection status

## Tech Stack

- **Framework**: React 19 with TypeScript
- **Build Tool**: Vite 7
- **Styling**: Tailwind CSS 4
- **Maps**: Leaflet + React Leaflet
- **State**: React Hooks
- **Live Updates**: Server-Sent Events (SSE)

## Development

### Prerequisites

- Node.js 18+
- npm or yarn
- QuetzalMap backend running on port 8080

### Setup

```bash
# Install dependencies
npm install

# Copy environment file
cp .env.example .env

# Start development server
npm run dev
```

The dev server will start on `http://localhost:3000` with API proxy to backend.

## Production Build

```bash
npm run build
```

Output in `dist/` directory.

## License

MIT License
