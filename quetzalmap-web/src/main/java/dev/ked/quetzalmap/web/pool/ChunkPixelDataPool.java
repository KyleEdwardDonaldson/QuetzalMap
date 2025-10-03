package dev.ked.quetzalmap.web.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Object pool for ChunkPixelData to avoid garbage collection overhead.
 * Provides garbage-free rendering by reusing pixel data objects.
 */
public final class ChunkPixelDataPool {
    private final BlockingQueue<ChunkPixelData> pool;
    private final int maxSize;

    public ChunkPixelDataPool(int poolSize) {
        this.pool = new ArrayBlockingQueue<>(poolSize);
        this.maxSize = poolSize;

        // Pre-allocate objects
        for (int i = 0; i < poolSize; i++) {
            pool.offer(new ChunkPixelData());
        }
    }

    /**
     * Acquire a ChunkPixelData object from the pool.
     * Creates a new one if pool is exhausted.
     */
    public ChunkPixelData acquire() {
        ChunkPixelData data = pool.poll();
        if (data == null) {
            // Pool exhausted - create temporary object
            return new ChunkPixelData();
        }
        return data;
    }

    /**
     * Release a ChunkPixelData object back to the pool.
     * Clears the data before returning.
     */
    public void release(ChunkPixelData data) {
        if (data == null) return;

        data.clear();

        // Only return to pool if not full
        pool.offer(data);
    }

    /**
     * Get current pool size.
     */
    public int getSize() {
        return pool.size();
    }

    /**
     * Get max pool size.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Check if pool is exhausted.
     */
    public boolean isExhausted() {
        return pool.isEmpty();
    }

    /**
     * Get memory usage estimate in bytes.
     */
    public long getMemoryUsage() {
        return (long) maxSize * 256 * 4; // 256 pixels * 4 bytes per pixel
    }
}
