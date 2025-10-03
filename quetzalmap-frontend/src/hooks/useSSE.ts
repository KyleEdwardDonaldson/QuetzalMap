import { useEffect, useRef, useState } from 'react';

export interface SSEEvent {
  type: string;
  data: any;
}

/**
 * Hook for Server-Sent Events (SSE) connection
 * Connects to the backend SSE endpoint and receives live updates
 */
export function useSSE(url: string, enabled: boolean = true) {
  const [events, setEvents] = useState<SSEEvent[]>([]);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    console.log('[SSE] Connecting to:', url);

    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      console.log('[SSE] Connected');
      setConnected(true);
      setError(null);
    };

    eventSource.onerror = (err) => {
      console.error('[SSE] Error:', err);
      setConnected(false);
      setError('Connection failed');
    };

    // Listen for specific event types
    const eventTypes = [
      'connected',
      'tile_updated',
      'marker_updated',
      'marker_removed',
      'player_moved'
    ];

    eventTypes.forEach(type => {
      eventSource.addEventListener(type, (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log(`[SSE] Event: ${type}`, data);

          setEvents(prev => [...prev, { type, data }]);
        } catch (err) {
          console.error(`[SSE] Failed to parse ${type} event:`, err);
        }
      });
    });

    // Cleanup on unmount
    return () => {
      console.log('[SSE] Disconnecting');
      eventSource.close();
    };
  }, [url, enabled]);

  const clearEvents = () => setEvents([]);

  return {
    events,
    connected,
    error,
    clearEvents
  };
}
