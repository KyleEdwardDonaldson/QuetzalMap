package dev.ked.quetzalmap.core.world;

import net.querz.nbt.io.NBTInputStream;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads Minecraft region files (.mca format).
 * Anvil region file parser for efficient chunk loading.
 */
public class MinecraftRegion {
    private static final Logger LOGGER = Logger.getLogger(MinecraftRegion.class.getName());
    private final Path regionFile;
    private final int regionX;
    private final int regionZ;
    private final Map<ChunkPos, MinecraftChunk> chunks = new HashMap<>();

    public MinecraftRegion(Path regionFile, int regionX, int regionZ) {
        this.regionFile = regionFile;
        this.regionX = regionX;
        this.regionZ = regionZ;
        LOGGER.fine("Created MinecraftRegion: file=" + regionFile + ", regionX=" + regionX + ", regionZ=" + regionZ);
    }

    /**
     * Load a chunk from the region file.
     */
    public MinecraftChunk getChunk(int chunkX, int chunkZ) throws IOException {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);

        if (chunks.containsKey(pos)) {
            LOGGER.fine("Chunk " + chunkX + "," + chunkZ + " found in cache");
            return chunks.get(pos);
        }

        LOGGER.fine("Loading chunk " + chunkX + "," + chunkZ + " from region file...");
        MinecraftChunk chunk = loadChunk(chunkX, chunkZ);
        if (chunk != null) {
            LOGGER.fine("Chunk " + chunkX + "," + chunkZ + " loaded successfully");
            chunks.put(pos, chunk);
        } else {
            LOGGER.fine("Chunk " + chunkX + "," + chunkZ + " does not exist in region");
        }
        return chunk;
    }

    private MinecraftChunk loadChunk(int chunkX, int chunkZ) throws IOException {
        // Calculate chunk position within region (0-31)
        int localX = chunkX & 31;
        int localZ = chunkZ & 31;

        LOGGER.fine("loadChunk: localX=" + localX + ", localZ=" + localZ);

        try (RandomAccessFile raf = new RandomAccessFile(regionFile.toFile(), "r")) {
            // Read chunk location from header
            int headerOffset = 4 * ((localX & 31) + (localZ & 31) * 32);
            raf.seek(headerOffset);

            int location = raf.readInt();
            LOGGER.fine("Chunk " + chunkX + "," + chunkZ + " location header: " + location);
            if (location == 0) {
                // Chunk doesn't exist in region - this is normal for ungenerated chunks
                return null;
            }

            int offset = (location >> 8) * 4096;
            int sectorCount = location & 0xFF;

            LOGGER.fine("Chunk " + chunkX + "," + chunkZ + " offset=" + offset + ", sectorCount=" + sectorCount);

            if (offset == 0 || sectorCount == 0) {
                // Invalid chunk data - silently skip
                return null;
            }

            // Read chunk data
            raf.seek(offset);
            int length = raf.readInt();
            byte compressionType = raf.readByte();

            LOGGER.fine("Chunk " + chunkX + "," + chunkZ + " length=" + length + ", compressionType=" + compressionType);

            if (length == 0 || compressionType == 0) {
                // Invalid chunk data - silently skip
                return null;
            }

            byte[] data = new byte[length - 1];
            raf.readFully(data);

            // Parse NBT - decompress and read
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            java.io.InputStream decompressed;

            if (compressionType == 1) {
                // GZip
                LOGGER.fine("Using GZip decompression");
                decompressed = new java.util.zip.GZIPInputStream(bais);
            } else if (compressionType == 2) {
                // Zlib
                LOGGER.fine("Using Zlib decompression");
                decompressed = new java.util.zip.InflaterInputStream(bais);
            } else {
                throw new IOException("Unsupported compression type: " + compressionType);
            }

            LOGGER.fine("Reading NBT data for chunk " + chunkX + "," + chunkZ + "...");
            try (NBTInputStream nbtIn = new NBTInputStream(decompressed)) {
                NamedTag namedTag = nbtIn.readTag(512);
                CompoundTag root = (CompoundTag) namedTag.getTag();
                LOGGER.fine("NBT data read successfully, creating MinecraftChunk...");
                return new MinecraftChunk(root, chunkX, chunkZ);
            }
        }
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    private record ChunkPos(int x, int z) {}
}
