package su.nezushin.nminimap.chunks;

import org.bukkit.Color;

/**
 * One open space (layer) of a column and the block drawn for it.
 *
 * @param drawY  Y of the drawn block (color + hillshade)
 * @param floorY Y of the solid block the space rests on
 * @param topY   highest passable Y of the space, or {@link LayerData#SKY} when nothing solid is above it
 */
public record BlockDataInfo(Color color, int drawY, int waterDepth, int floorY, int topY) {

}
