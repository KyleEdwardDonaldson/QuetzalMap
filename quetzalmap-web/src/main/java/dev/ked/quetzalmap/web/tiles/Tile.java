package dev.ked.quetzalmap.web.tiles;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a map tile with zero-copy pixel update capabilities.
 * Uses direct pixel array access to avoid BufferedImage copying overhead.
 *
 * Thread-safe for concurrent reads and exclusive writes.
 */
public final class Tile {
    public static final int TILE_SIZE = 512;
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNKS_PER_TILE = TILE_SIZE / CHUNK_SIZE; // 32 chunks per tile

    private final TileCoord coord;
    private final BufferedImage image;
    private final int[] pixels; // Direct pixel array (zero-copy)
    private final ReadWriteLock lock;

    private volatile boolean dirty;
    private volatile long lastModified;

    public Tile(TileCoord coord) {
        this.coord = coord;
        this.image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        this.lock = new ReentrantReadWriteLock();
        this.dirty = true;
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Update a 16×16 chunk region within this tile.
     * Uses zero-copy array operations for maximum performance.
     *
     * @param chunkX Chunk X coordinate within tile (0-31)
     * @param chunkZ Chunk Z coordinate within tile (0-31)
     * @param chunkPixels 256-length array of ARGB pixels
     */
    public void updateChunkPixels(int chunkX, int chunkZ, int[] chunkPixels) {
        if (chunkPixels.length != CHUNK_SIZE * CHUNK_SIZE) {
            throw new IllegalArgumentException("Chunk pixels must be 256 elements (16×16)");
        }

        lock.writeLock().lock();
        try {
            int offsetX = chunkX * CHUNK_SIZE;
            int offsetZ = chunkZ * CHUNK_SIZE;

            // Copy 16 rows of 16 pixels each
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int srcPos = z * CHUNK_SIZE;
                int dstPos = (offsetZ + z) * TILE_SIZE + offsetX;
                System.arraycopy(chunkPixels, srcPos, pixels, dstPos, CHUNK_SIZE);
            }

            this.dirty = true;
            this.lastModified = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Set all pixels in the tile at once.
     *
     * @param allPixels 512×512 pixel array (262,144 elements)
     */
    public void setAllPixels(int[] allPixels) {
        if (allPixels.length != TILE_SIZE * TILE_SIZE) {
            throw new IllegalArgumentException("Must provide exactly " + (TILE_SIZE * TILE_SIZE) + " pixels");
        }

        lock.writeLock().lock();
        try {
            System.arraycopy(allPixels, 0, pixels, 0, pixels.length);
            this.dirty = true;
            this.lastModified = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get a thread-safe copy of the image for rendering/saving.
     * Holds a read lock during the copy operation.
     */
    public BufferedImage getImage() {
        lock.readLock().lock();
        try {
            return copyImage();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get direct read access to pixels (thread-safe).
     * Caller must hold the read lock.
     */
    public int[] getPixels() {
        return pixels;
    }

    /**
     * Get the read lock for manual pixel access.
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    public TileCoord getCoord() {
        return coord;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public long getLastModified() {
        return lastModified;
    }

    /**
     * Get memory size estimate in bytes.
     */
    public long getMemorySize() {
        // Image data + metadata
        return (TILE_SIZE * TILE_SIZE * 4) + 256; // 4 bytes per pixel + overhead
    }

    private BufferedImage copyImage() {
        BufferedImage copy = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] copyPixels = ((DataBufferInt) copy.getRaster().getDataBuffer()).getData();
        System.arraycopy(pixels, 0, copyPixels, 0, pixels.length);
        return copy;
    }

    @Override
    public String toString() {
        return "Tile{" + coord + ", dirty=" + dirty + ", lastModified=" + lastModified + "}";
    }
}
