package com.mew.mewhome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class HomeManager extends SavedData {
    private static final String DATA_NAME = "mewhome_homes";
    private final Map<UUID, HomeData> homes = new HashMap<>();

    public record HomeData(BlockPos pos, ResourceKey<Level> dimension) {}

    public HomeManager() {}

    public void setHome(UUID playerId, BlockPos pos, ResourceKey<Level> dimension) {
        homes.put(playerId, new HomeData(pos, dimension));
        setDirty();
    }

    public Optional<HomeData> getHome(UUID playerId) {
        return Optional.ofNullable(homes.get(playerId));
    }

    public void removeHome(UUID playerId) {
        if (homes.remove(playerId) != null) {
            setDirty();
        }
    }

    /**
     * Find the owner of a home bed at the given HEAD position.
     */
    public Optional<UUID> getHomeOwnerAt(BlockPos headPos, ResourceKey<Level> dimension) {
        return homes.entrySet().stream()
                .filter(e -> e.getValue().dimension().equals(dimension) && e.getValue().pos().equals(headPos))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag homesTag = new CompoundTag();
        for (Map.Entry<UUID, HomeData> entry : homes.entrySet()) {
            CompoundTag homeTag = new CompoundTag();
            BlockPos pos = entry.getValue().pos();
            homeTag.putInt("x", pos.getX());
            homeTag.putInt("y", pos.getY());
            homeTag.putInt("z", pos.getZ());
            homeTag.putString("dimension", entry.getValue().dimension().location().toString());
            homesTag.put(entry.getKey().toString(), homeTag);
        }
        tag.put("homes", homesTag);
        return tag;
    }

    public static HomeManager load(CompoundTag tag, HolderLookup.Provider provider) {
        HomeManager manager = new HomeManager();
        CompoundTag homesTag = tag.getCompound("homes");
        for (String key : homesTag.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            CompoundTag homeTag = homesTag.getCompound(key);
            BlockPos pos = new BlockPos(homeTag.getInt("x"), homeTag.getInt("y"), homeTag.getInt("z"));
            ResourceKey<Level> dim = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(homeTag.getString("dimension"))
            );
            manager.homes.put(uuid, new HomeData(pos, dim));
        }
        return manager;
    }

    public static HomeManager get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(HomeManager::new, HomeManager::load, null), DATA_NAME);
    }
}
