package com.mew.mewhome;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

@EventBusSubscriber(modid = MewHome.MODID)
public class HomeCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /home — teleport to your bed
        dispatcher.register(Commands.literal("home")
                .executes(ctx -> executeHome(ctx.getSource().getPlayerOrException()))
        );

        // /sethome — inform player about bed mechanic
        dispatcher.register(Commands.literal("sethome")
                .executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    sp.sendSystemMessage(ServerI18n.translate(sp, "mewhome.message.sethome_info", ChatFormatting.YELLOW));
                    return 1;
                })
        );

        // /spawn — teleport to world spawn
        dispatcher.register(Commands.literal("spawn")
                .executes(ctx -> executeSpawn(ctx.getSource().getPlayerOrException()))
        );
    }

    private static int executeHome(ServerPlayer player) {
        HomeManager manager = HomeManager.get(player.server);
        Optional<HomeManager.HomeData> homeOpt = manager.getHome(player.getUUID());

        if (homeOpt.isEmpty()) {
            player.sendSystemMessage(ServerI18n.translate(player, "mewhome.message.no_home", ChatFormatting.RED));
            return 0;
        }

        HomeManager.HomeData home = homeOpt.get();
        ServerLevel level = player.server.getLevel(home.dimension());
        if (level == null) {
            player.sendSystemMessage(ServerI18n.translate(player, "mewhome.message.world_unavailable", ChatFormatting.RED));
            return 0;
        }

        // Validate bed still exists
        BlockState bedState = level.getBlockState(home.pos());
        if (!(bedState.getBlock() instanceof BedBlock)) {
            manager.removeHome(player.getUUID());
            player.sendSystemMessage(ServerI18n.translate(player, "mewhome.message.no_home", ChatFormatting.RED));
            return 0;
        }

        // Find safe standing position near the bed
        Direction facing = bedState.getValue(BedBlock.FACING);
        Optional<Vec3> standUpPos = BedBlock.findStandUpPosition(
                EntityType.PLAYER, level, home.pos(), facing, 0f
        );

        Vec3 teleportPos = standUpPos.orElse(Vec3.atBottomCenterOf(home.pos().above()));

        player.teleportTo(level, teleportPos.x, teleportPos.y, teleportPos.z, player.getYRot(), player.getXRot());

        player.sendSystemMessage(ServerI18n.translate(player, "mewhome.message.teleported", ChatFormatting.GREEN));
        return 1;
    }

    private static int executeSpawn(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5;

        player.teleportTo(overworld, x, y, z, player.getYRot(), player.getXRot());
        player.sendSystemMessage(ServerI18n.translate(player, "mewhome.message.spawn_tp", ChatFormatting.GREEN));
        return 1;
    }
}
