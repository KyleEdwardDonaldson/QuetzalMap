package dev.ked.quetzalmap.core.world;

/**
 * Represents a block state at a specific location.
 */
public class BlockState {
    private final BlockType type;
    private final int y;

    public BlockState(BlockType type, int y) {
        this.type = type;
        this.y = y;
    }

    public BlockType getType() {
        return type;
    }

    public int getY() {
        return y;
    }
}
