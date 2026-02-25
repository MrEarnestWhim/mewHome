package com.mew.mewhome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = MewHome.MODID)
public class HomeEventHandler {

    // =========================================================================
    // Bed Placement → Set Home
    // =========================================================================
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock() instanceof BedBlock)) return;

        BlockPos pos = event.getPos();
        BedPart part = state.getValue(BedBlock.PART);
        Direction facing = state.getValue(BedBlock.FACING);

        // We store the HEAD position (vanilla uses head for respawn logic)
        BlockPos headPos = (part == BedPart.FOOT) ? pos.relative(facing) : pos;

        ResourceKey<Level> dimension = player.level().dimension();
        HomeManager manager = HomeManager.get(player.server);
        manager.setHome(player.getUUID(), headPos, dimension);

        player.sendSystemMessage(ServerI18n.translate(player, "mewhome.message.home_set", ChatFormatting.GREEN));
    }

    // =========================================================================
    // Cancel ALL vanilla spawn setting (bed click must NOT set spawn)
    // =========================================================================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        event.setCanceled(true);
    }

    // =========================================================================
    // Bed Breaking → Protect home beds (only OP can break)
    // =========================================================================
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof BedBlock)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BedPart part = state.getValue(BedBlock.PART);
        Direction facing = state.getValue(BedBlock.FACING);

        // Calculate HEAD position from whichever part is being broken
        BlockPos headPos = (part == BedPart.FOOT) ? pos.relative(facing) : pos;

        ResourceKey<Level> dimension = serverLevel.dimension();
        HomeManager manager = HomeManager.get(serverLevel.getServer());

        // Check if this bed is anyone's home
        Optional<UUID> owner = manager.getHomeOwnerAt(headPos, dimension);
        if (owner.isEmpty()) return; // Not a home bed — allow breaking normally

        // Only OP (permission level 2+) can break home beds
        if (!event.getPlayer().hasPermissions(2)) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(ServerI18n.translate(sp, "mewhome.message.bed_protected", ChatFormatting.RED));
            }
            return;
        }

        // OP is breaking it — clear the owner's home data
        UUID ownerId = owner.get();
        manager.removeHome(ownerId);

        // Notify the owner if online
        ServerPlayer ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        if (ownerPlayer != null) {
            ownerPlayer.sendSystemMessage(
                    ServerI18n.translate(ownerPlayer, "mewhome.message.bed_broken", ChatFormatting.RED)
            );
        }
    }

    // =========================================================================
    // Respawn → Redirect to stored home bed
    // =========================================================================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isFromEndFight()) return; // Don't touch End fight respawn

        HomeManager manager = HomeManager.get(player.server);
        Optional<HomeManager.HomeData> homeOpt = manager.getHome(player.getUUID());
        if (homeOpt.isEmpty()) return; // No home — vanilla world spawn

        HomeManager.HomeData home = homeOpt.get();
        ServerLevel level = player.server.getLevel(home.dimension());
        if (level == null) return;

        // Validate bed still exists at stored position
        BlockState bedState = level.getBlockState(home.pos());
        if (!(bedState.getBlock() instanceof BedBlock)) {
            // Bed was destroyed (explosion, piston, etc.) — clear home
            manager.removeHome(player.getUUID());
            return; // Fall through to world spawn
        }

        // Find safe standing position near the bed
        Direction facing = bedState.getValue(BedBlock.FACING);
        Optional<Vec3> standUpPos = BedBlock.findStandUpPosition(
                EntityType.PLAYER, level, home.pos(), facing, 0f
        );

        if (standUpPos.isPresent()) {
            Vec3 respawnPos = standUpPos.get();
            event.setDimensionTransition(new DimensionTransition(
                    level, respawnPos, Vec3.ZERO, 0f, 0f, DimensionTransition.DO_NOTHING
            ));
        }
    }
}
