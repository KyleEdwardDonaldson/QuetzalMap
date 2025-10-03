package dev.ked.quetzalmap.web.rendering;

import dev.ked.quetzalmap.core.world.BlockState;
import dev.ked.quetzalmap.core.world.BlockType;

/**
 * Calculates pixel colors for map rendering.
 * Handles heightmap shading and transparency.
 */
public final class ColorCalculator {

    /**
     * Calculate the pixel color for a block.
     */
    public static int calculatePixelColor(BlockState blockState) {
        if (blockState == null || blockState.getType() == null) {
            return 0;
        }

        BlockType type = blockState.getType();
        int pixelColor = type.getColor();

        if (pixelColor == 0) {
            return 0;
        }

        // Set full alpha
        pixelColor = setAlpha(0xFF, pixelColor);

        // Apply heightmap shading
        pixelColor = applyHeightmapShading(pixelColor, blockState.getY());

        // Apply transparency for glass/ice
        if (type.isTransparent()) {
            pixelColor = setAlpha(0xAA, pixelColor);
        }

        return pixelColor;
    }

    /**
     * Set alpha channel.
     */
    private static int setAlpha(int alpha, int color) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Blend two colors with alpha.
     */
    private static int blend(int top, int bottom) {
        int alphaTop = (top >> 24) & 0xFF;
        if (alphaTop == 0) return bottom;
        if (alphaTop == 0xFF) return top;

        int alphaBottom = (bottom >> 24) & 0xFF;
        int alphaOut = alphaTop + alphaBottom * (255 - alphaTop) / 255;

        int rTop = (top >> 16) & 0xFF;
        int gTop = (top >> 8) & 0xFF;
        int bTop = top & 0xFF;

        int rBottom = (bottom >> 16) & 0xFF;
        int gBottom = (bottom >> 8) & 0xFF;
        int bBottom = bottom & 0xFF;

        int rOut = (rTop * alphaTop + rBottom * alphaBottom * (255 - alphaTop) / 255) / alphaOut;
        int gOut = (gTop * alphaTop + gBottom * alphaBottom * (255 - alphaTop) / 255) / alphaOut;
        int bOut = (bTop * alphaTop + bBottom * alphaBottom * (255 - alphaTop) / 255) / alphaOut;

        return (alphaOut << 24) | (rOut << 16) | (gOut << 8) | bOut;
    }

    /**
     * Apply heightmap-based shading to create depth perception.
     */
    private static int applyHeightmapShading(int color, int blockY) {
        // Simple heightmap shading based on block Y
        // Higher blocks are brighter, lower blocks are darker
        int worldHeight = 384; // 1.21 world height (-64 to 320)
        int minHeight = -64;

        double heightFactor = (double) (blockY - minHeight) / worldHeight;
        heightFactor = Math.max(0.0, Math.min(1.0, heightFactor));

        // Apply subtle shading (±10% brightness)
        double shadeFactor = 0.9 + (heightFactor * 0.2);

        int r = (int) (((color >> 16) & 0xFF) * shadeFactor);
        int g = (int) (((color >> 8) & 0xFF) * shadeFactor);
        int b = (int) ((color & 0xFF) * shadeFactor);

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
