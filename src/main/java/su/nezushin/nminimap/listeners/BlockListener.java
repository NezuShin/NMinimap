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
import org.bukkit.event.inventory.FurnaceExtractEvent;
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

    /**
     * @param structural the change can add or remove an open space, so it matters to cave layers even without sky light
     */
    public void update(Block b, boolean structural) {
        if (b == null)
            return;
        if ((structural && Config.multiLayerMaxLayers > 1) || b.getLightFromSky() == 15
                || (b.getWorld().hasCeiling() && PerWorldSettingsUtil.getSkipCeiling(b.getWorld())))
            NMinimap.async(() -> {
                NMinimap.getInstance().getChunkManager().reRenderChunk(new ChunkEntry(b.getWorld().getName(), Math.floorDiv(b.getX(), 16), Math.floorDiv(b.getZ(), 16), null));
            });
    }

    /** Frequent events that don't add or remove solid blocks. */
    private static boolean isNoisy(Event event) {
        return event instanceof BlockRedstoneEvent || event instanceof MoistureChangeEvent
                || event instanceof BlockFertilizeEvent || event instanceof BlockIgniteEvent
                || event instanceof FurnaceExtractEvent
                // BlockFormEvent (and BlockSpreadEvent, EntityBlockFormEvent) extends BlockGrowEvent
                || (event instanceof BlockGrowEvent && !(event instanceof BlockFormEvent));
    }


    private void onBlockChange(Event event) {
        if (event instanceof BlockPhysicsEvent)
            return;

        final boolean structural = !isNoisy(event);

        if (event instanceof BlockExplodeEvent explode) {
            explode.blockList().forEach(b -> update(b, structural));
        } else if (event instanceof EntityExplodeEvent explode) {
            explode.blockList().forEach(b -> update(b, structural));
        } else if (event instanceof EntityChangeBlockEvent changeBlock) {
            update(changeBlock.getBlock(), structural);
        } else if (event instanceof BlockBurnEvent burn) {
            update(burn.getIgnitingBlock(), structural);
        } else if (event instanceof BlockFertilizeEvent fertilize) {
            fertilize.getBlocks().stream()
                    .filter(BlockState::isPlaced)
                    .map(BlockState::getBlock)
                    .forEach(b -> update(b, structural));
        } else if (event instanceof BlockFromToEvent fromTo) {
            update(fromTo.getToBlock(), structural);
        } else if (event instanceof BlockIgniteEvent ignite) {
            update(ignite.getIgnitingBlock(), structural);
        } else if (event instanceof BlockPistonExtendEvent pistonExtend) {
            pistonExtend.getBlocks().forEach(b -> update(b, structural));
        } else if (event instanceof BlockPistonRetractEvent pistonRetract) {
            pistonRetract.getBlocks().forEach(b -> update(b, structural));
        } else if (event instanceof SpongeAbsorbEvent spongeAbsorb) {
            spongeAbsorb.getBlocks().stream()
                    .filter(BlockState::isPlaced)
                    .map(BlockState::getBlock)
                    .forEach(b -> update(b, structural));
        } else if (event instanceof TNTPrimeEvent tntPrime) {
            update(tntPrime.getPrimingBlock(), structural);
        }
        if (event instanceof BlockEvent blockEvent) {
            update(blockEvent.getBlock(), structural);
        }

    }
}
