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
import su.nezushin.nminimap.util.SchedulerUtil;
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
        if (b.getLightFromSky() == 15 || (b.getWorld().hasCeiling() && Config.skipCeiling))
            NMinimap.async(() -> {
                NMinimap.getInstance().getChunkManager().reRenderChunk(new ChunkEntry(b.getWorld(), Math.floorDiv(b.getX(), 16), Math.floorDiv(b.getZ(), 16)));
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
