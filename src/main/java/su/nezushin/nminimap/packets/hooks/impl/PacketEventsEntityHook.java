package su.nezushin.nminimap.packets.hooks.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.google.common.collect.Lists;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import su.nezushin.nminimap.packets.hooks.EntityHook;
import su.nezushin.nminimap.util.ChunkLoadingUtil;
import su.nezushin.nminimap.util.PacketEventsUtil;

import java.util.List;
import java.util.UUID;

public class PacketEventsEntityHook implements EntityHook {

    @Override
    public void spawnMarker(Player p, int id) {
        PacketEventsUtil.sendPackets(p, new WrapperPlayServerSpawnEntity(id, UUID.randomUUID(),
                SpigotConversionUtil.fromBukkitEntityType(EntityType.TEXT_DISPLAY),
                SpigotConversionUtil.fromBukkitLocation(p.getLocation()), 0.0f, 0, null));
    }

    @Override
    public void spawnItemFrame(Player p, int id, ItemStack item, boolean direction) {

        PacketEventsUtil.sendPackets(p, new WrapperPlayServerSpawnEntity(id, UUID.randomUUID(),
                        SpigotConversionUtil.fromBukkitEntityType(EntityType.ITEM_FRAME),
                        SpigotConversionUtil.fromBukkitLocation(p.getLocation()), 0.0f, 0, null),
                new WrapperPlayServerEntityMetadata(id, Lists.newArrayList(
                        new EntityData(9, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(item)),
                        new EntityData(8, EntityDataTypes.BLOCK_FACE, BlockFace.getBlockFaceByValue(direction ? 0 : 1)),
                        new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20)
                )));
    }

    @Override
    public void teleportItemFrame(Player p, int id) {
        var loc = p.getEyeLocation().add(0, -1, 0);
        var face = p.getFacing().getOppositeFace();

        PacketEventsUtil.sendPackets(p, new WrapperPlayServerEntityTeleport(id, SpigotConversionUtil.fromBukkitLocation(loc.add(p.getLocation().getDirection().multiply(3))), false)
                , new WrapperPlayServerEntityMetadata(id, Lists.newArrayList(
                        new EntityData(8, EntityDataTypes.BLOCK_FACE, BlockFace.getBlockFaceByValue(PacketEventsUtil.getBlockFace(face)))
                )));
    }


    @Override
    public void removeEntities(Player p, int... ids) {
        PacketEventsUtil.sendPackets(p, new WrapperPlayServerDestroyEntities(ids));
    }

    @Override
    public void sendMapData(Player p, int mapId, int scale, byte[] mapData) {
        PacketEventsUtil.sendPackets(p, new WrapperPlayServerMapData(mapId, (byte) scale, false,
                false, Lists.newArrayList(),
                128, 128, 0, 0, mapData));
    }

    @Override
    public void sendMarkerData(Player p, int id, Component markerData) {
        List<EntityData<?>> list = Lists.newArrayList(
                new EntityData(25, EntityDataTypes.INT, 0x000000FF),
                new EntityData(27, EntityDataTypes.BYTE, (byte) 0x00),
                new EntityData(15, EntityDataTypes.BYTE, (byte) 3)
        );

        if (ChunkLoadingUtil.isPaper()) {
            list.add(new EntityData(23, EntityDataTypes.ADV_COMPONENT, markerData));
        } else {
            list.add(new EntityData(23, EntityDataTypes.COMPONENT, JSONComponentSerializer.json().serialize(markerData)));
        }

        PacketEvents.getAPI().getPlayerManager().sendPacket(p, new WrapperPlayServerEntityMetadata(id, list));
    }
}
