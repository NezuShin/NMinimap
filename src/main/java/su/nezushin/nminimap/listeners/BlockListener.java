package su.nezushin.nminimap.listeners;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.config.Config;

import java.util.Set;

public class BlockListener implements Listener {


    //Taken from https://github.com/JNNGL/VanillaMinimaps/blob/main/src/main/java/com/jnngl/vanillaminimaps/listener/MinimapBlockListener.java
    private static final Set<Class<? extends Event>> EVENTS =
            Set.of(BlockBurnEvent.class, BlockExpEvent.class, BlockExplodeEvent.class, BlockFadeEvent.class,
                    BlockFertilizeEvent.class, BlockFromToEvent.class, BlockGrowEvent.class, BlockIgniteEvent.class,
                    BlockPistonExtendEvent.class, BlockPistonRetractEvent.class, BlockPlaceEvent.class, BlockPhysicsEvent.class,
                    BlockRedstoneEvent.class, FluidLevelChangeEvent.class, LeavesDecayEvent.class, MoistureChangeEvent.class,
                    SculkBloomEvent.class, SpongeAbsorbEvent.class, TNTPrimeEvent.class, EntityBlockFormEvent.class,
                    BlockFormEvent.class, BlockSpreadEvent.class, EntityExplodeEvent.class, EntityChangeBlockEvent.class);

    public void registerListener() {
        EVENTS.forEach(eventClass ->
                Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.HIGH,
                        (listener, event) -> onBlockChange(event), NMinimap.getInstance()));
    }

    public void update(Block b) {
        if (b == null)
            return;
        boolean updateSurface = b.getLightFromSky() == 15 || (b.getWorld().hasCeiling() && PerWorldSettingsUtil.getSkipCeiling(b.getWorld()));
        var world = b.getWorld().getName();
        var chunkX = Math.floorDiv(b.getX(), 16);
        var chunkZ = Math.floorDiv(b.getZ(), 16);
        if (updateSurface || !Config.undergroundLayers.isEmpty())
            NMinimap.async(() -> {
                var manager = NMinimap.getInstance().getChunkManager();
                if (updateSurface)
                    manager.reRenderChunk(new ChunkEntry(world, chunkX, chunkZ, null));
                for (var layer : Config.undergroundLayers) {
                    int radius = layer.smartDescend().enabled() && layer.smartDescend().minConnectedColumns() > 1 ? 1 : 0;
                    for (int dx = -radius; dx <= radius; dx++)
                        for (int dz = -radius; dz <= radius; dz++) {
                            var entry = new ChunkEntry(world, chunkX + dx, chunkZ + dz, layer);
                            if (manager.getLoadedTiles().containsKey(entry) || manager.getChunkCache().hasInCache(entry))
                                manager.reRenderChunk(entry);
                        }
                }
            });
    }


    private void onBlockChange(Event event) {
        if (event instanceof BlockPhysicsEvent)
            return;

        if (event instanceof BlockExplodeEvent explode) {
            explode.blockList().forEach(this::update);
        } else if (event instanceof EntityExplodeEvent explode) {
            explode.blockList().forEach(this::update);
        } else if (event instanceof EntityChangeBlockEvent changeBlock) {
            update(changeBlock.getBlock());
        } else if (event instanceof BlockBurnEvent burn) {
            update(burn.getIgnitingBlock());
        } else if (event instanceof BlockFertilizeEvent fertilize) {
            fertilize.getBlocks().stream()
                    .filter(BlockState::isPlaced)
                    .map(BlockState::getBlock)
                    .forEach(this::update);
        } else if (event instanceof BlockFromToEvent fromTo) {
            update(fromTo.getToBlock());
        } else if (event instanceof BlockIgniteEvent ignite) {
            update(ignite.getIgnitingBlock());
        } else if (event instanceof BlockPistonExtendEvent pistonExtend) {
            pistonExtend.getBlocks().forEach(this::update);
        } else if (event instanceof BlockPistonRetractEvent pistonRetract) {
            pistonRetract.getBlocks().forEach(this::update);
        } else if (event instanceof SpongeAbsorbEvent spongeAbsorb) {
            spongeAbsorb.getBlocks().stream()
                    .filter(BlockState::isPlaced)
                    .map(BlockState::getBlock)
                    .forEach(this::update);
        } else if (event instanceof TNTPrimeEvent tntPrime) {
            update(tntPrime.getPrimingBlock());
        }
        if (event instanceof BlockEvent blockEvent) {
            update(blockEvent.getBlock());
        }

    }
}
