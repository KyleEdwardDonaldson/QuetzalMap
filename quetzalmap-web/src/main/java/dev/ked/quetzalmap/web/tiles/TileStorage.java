package dev.ked.quetzalmap.web.tiles;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles disk I/O for map tiles.
 * Saves tiles as PNG files organized by world/zoom/x_z.png.
 */
public final class TileStorage {
    private static final Logger LOGGER = Logger.getLogger(TileStorage.class.getName());

    private final Path tilesDirectory;

    public TileStorage(Path tilesDirectory) {
        this.tilesDirectory = tilesDirectory.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.tilesDirectory);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create tiles directory", e);
        }
    }

    /**
     * Save a tile to disk as PNG.
     */
    public boolean save(Tile tile) {
        Path tilePath = getTilePath(tile.getCoord());

        try {
            // Create parent directories
            Path parentPath = tilePath.getParent();
            if (parentPath == null) {
                LOGGER.severe("Tile path has no parent! Path: " + tilePath);
                return false;
            }
            Files.createDirectories(parentPath);

            // Write to temporary file first (atomic operation)
            Path tempPath = tilePath.resolveSibling(tilePath.getFileName() + ".tmp");
            BufferedImage image = tile.getImage();

            // Save as PNG with compression (30-50% smaller files)
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(tempPath.toFile())) {
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
                if (!writers.hasNext()) {
                    throw new IOException("No PNG writer available");
                }

                ImageWriter writer = writers.next();
                writer.setOutput(ios);

                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.85f); // High quality, good compression
                }

                writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
                writer.dispose();
            }

            // Atomic move
            Files.move(tempPath, tilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            tile.markClean();
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save tile: " + tile.getCoord(), e);
            return false;
        }
    }

    /**
     * Load a tile from disk.
     * Returns null if file doesn't exist or can't be loaded.
     */
    public Tile load(TileCoord coord) {
        Path tilePath = getTilePath(coord);

        if (!Files.exists(tilePath)) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(tilePath.toFile());
            if (image == null) {
                LOGGER.warning("Failed to read tile image: " + coord);
                return null;
            }

            // Create tile and copy pixels
            Tile tile = new Tile(coord);
            int[] loadedPixels = image.getRGB(0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE, null, 0, Tile.TILE_SIZE);
            tile.setAllPixels(loadedPixels);
            tile.markClean();

            return tile;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load tile: " + coord, e);
            return null;
        }
    }

    /**
     * Delete a tile from disk.
     */
    public boolean delete(TileCoord coord) {
        Path tilePath = getTilePath(coord);

        try {
            return Files.deleteIfExists(tilePath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete tile: " + coord, e);
            return false;
        }
    }

    /**
     * Check if a tile exists on disk.
     */
    public boolean exists(TileCoord coord) {
        return Files.exists(getTilePath(coord));
    }

    /**
     * Get the last modified time of a tile.
     */
    public long getLastModified(TileCoord coord) {
        Path tilePath = getTilePath(coord);
        try {
            return Files.getLastModifiedTime(tilePath).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Get the file size of a tile in bytes.
     */
    public long getSize(TileCoord coord) {
        Path tilePath = getTilePath(coord);
        try {
            return Files.size(tilePath);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Get the path to a tile file.
     * Format: tiles/{world}/{zoom}/{x}_{z}.png
     */
    private Path getTilePath(TileCoord coord) {
        return tilesDirectory
                .resolve(coord.getWorld())
                .resolve(String.valueOf(coord.getZoom()))
                .resolve(coord.getFileName());
    }

    public Path getTilesDirectory() {
        return tilesDirectory;
    }
}
