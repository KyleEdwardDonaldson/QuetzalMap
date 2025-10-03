package dev.ked.quetzalmap.web.pool;

/**
 * Represents pixel data for a 16×16 chunk (256 pixels).
 * Pooled to avoid garbage collection overhead during rendering.
 */
public final class ChunkPixelData {
    private static final int CHUNK_SIZE = 16;
    private static final int PIXEL_COUNT = CHUNK_SIZE * CHUNK_SIZE;

    private final int[] pixels;
    private int chunkX;
    private int chunkZ;

    public ChunkPixelData() {
        this.pixels = new int[PIXEL_COUNT];
    }

    public int[] getPixels() {
        return pixels;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public void setChunkCoords(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /**
     * Set a pixel at the given position within the chunk.
     */
    public void setPixel(int x, int z, int color) {
        pixels[(z & 0xF) * CHUNK_SIZE + (x & 0xF)] = color;
    }

    /**
     * Clear pixel data (reset to transparent black).
     */
    public void clear() {
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0;
        }
    }

    /**
     * Get memory size in bytes.
     */
    public int getMemorySize() {
        return PIXEL_COUNT * 4; // 4 bytes per pixel
    }
}
